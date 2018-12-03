/*
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.index.labelscan;

import org.eclipse.collections.api.iterator.LongIterator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.neo4j.collection.PrimitiveLongResourceIterator;
import org.neo4j.cursor.RawCursor;
import org.neo4j.index.internal.gbptree.GBPTree;
import org.neo4j.index.internal.gbptree.Hit;
import org.neo4j.kernel.api.index.IndexProgressor;
import org.neo4j.kernel.api.labelscan.LabelScan;
import org.neo4j.kernel.api.labelscan.LabelScanReader;

import static org.neo4j.kernel.impl.index.labelscan.LabelScanValue.RANGE_SIZE;
import static org.neo4j.kernel.impl.index.labelscan.NativeLabelScanWriter.rangeOf;

/**
 * {@link LabelScanReader} for reading data from {@link NativeLabelScanStore}.
 * Each {@link LongIterator} returned from each of the methods is backed by {@link RawCursor}
 * directly from {@link GBPTree#seek(Object, Object)}.
 */
class NativeLabelScanReader implements LabelScanReader
{
    /**
     * Index which is queried when calling the methods below.
     */
    private final GBPTree<LabelScanKey,LabelScanValue> index;

    NativeLabelScanReader( GBPTree<LabelScanKey,LabelScanValue> index )
    {
        this.index = index;
    }

    @Override
    public PrimitiveLongResourceIterator nodesWithLabel( int labelId )
    {
        RawCursor<Hit<LabelScanKey,LabelScanValue>,IOException> cursor;
        try
        {
            cursor = seekerForLabel( 0, labelId );
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }

        return new LabelScanValueIterator( cursor, NO_ID );
    }

    @Override
    public PrimitiveLongResourceIterator nodesWithAnyOfLabels( long fromId, int... labelIds )
    {
        List<PrimitiveLongResourceIterator> iterators = iteratorsForLabels( fromId, labelIds );
        return new CompositeLabelScanValueIterator( iterators, false );
    }

    @Override
    public LabelScan nodeLabelScan( int labelId )
    {
        return new NativeLabelScan( labelId );
    }

    private List<PrimitiveLongResourceIterator> iteratorsForLabels( long fromId, int[] labelIds )
    {
        List<PrimitiveLongResourceIterator> iterators = new ArrayList<>();
        try
        {
            for ( int labelId : labelIds )
            {
                RawCursor<Hit<LabelScanKey,LabelScanValue>,IOException> cursor = seekerForLabel( fromId, labelId );
                iterators.add( new LabelScanValueIterator( cursor, fromId ) );
            }
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
        return iterators;
    }

    private RawCursor<Hit<LabelScanKey,LabelScanValue>,IOException> seekerForLabel( long startId, int labelId ) throws IOException
    {
        LabelScanKey from = new LabelScanKey( labelId, rangeOf( startId ) );
        LabelScanKey to = new LabelScanKey( labelId, Long.MAX_VALUE );
        return index.seek( from, to );
    }

    private RawCursor<Hit<LabelScanKey,LabelScanValue>,IOException> seekerForLabel( long startId, long stopId, int labelId ) throws IOException
    {
        LabelScanKey from = new LabelScanKey( labelId, rangeOf( startId ) );
        LabelScanKey to = new LabelScanKey( labelId, rangeOf( stopId ) );

        return index.seek( from, to );
    }

    private class NativeLabelScan implements LabelScan
    {
        private final AtomicLong nextStart;
        private final int labelId;

        NativeLabelScan( int labelId )
        {
            this.labelId = labelId;
            nextStart = new AtomicLong( 0 );
        }

        @Override
        public IndexProgressor initialize( IndexProgressor.NodeLabelClient client )
        {
            return init( client, 0L, Long.MAX_VALUE );
        }

        @Override
        public IndexProgressor initializeBatch( IndexProgressor.NodeLabelClient client, int sizeHint, long upperBound  )
        {
            if ( sizeHint == 0 )
            {
                return IndexProgressor.EMPTY;
            }
            long size = roundUp( sizeHint );
            long start = nextStart.getAndAdd( size );
            long max = roundUp( upperBound );
            long stop = Math.min( start + size, max );
            if ( start >= max )
            {
                return IndexProgressor.EMPTY;
            }
            return init( client, start, stop );
        }

        private IndexProgressor init( IndexProgressor.NodeLabelClient client, long start, long stop )
        {
            RawCursor<Hit<LabelScanKey,LabelScanValue>,IOException> cursor;
            try
            {
                cursor = seekerForLabel( start, stop, labelId );
            }
            catch ( IOException e )
            {
                throw new UncheckedIOException( e );
            }

            return new LabelScanValueIndexProgressor( cursor, client );
        }

        private long roundUp( long sizeHint )
        {
            return (sizeHint / RANGE_SIZE + 1) * RANGE_SIZE;
        }
    }
}
