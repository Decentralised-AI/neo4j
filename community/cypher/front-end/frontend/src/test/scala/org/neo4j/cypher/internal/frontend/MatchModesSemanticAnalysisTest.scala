/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.frontend

import org.neo4j.cypher.internal.ast.semantics.SemanticError
import org.neo4j.cypher.internal.ast.semantics.SemanticFeature
import org.neo4j.cypher.internal.util.test_helpers.CypherFunSuite
import org.neo4j.cypher.internal.util.test_helpers.TestName
import org.neo4j.cypher.internal.util.InputPosition

class MatchModesSemanticAnalysisTest extends CypherFunSuite with SemanticAnalysisTestSuiteWithDefaultQuery
    with TestName {

  override def defaultQuery: String = s"MATCH $testName RETURN *"

  val semanticFeatures = Seq(
    SemanticFeature.QuantifiedPathPatterns,
    SemanticFeature.GpmShortestPath
  )

  def semanticErrorWithPos(pos: InputPosition): SemanticError = SemanticError(
    "Match mode \"REPEATABLE ELEMENTS\" was used, but pattern is not bounded.",
    pos
  )

  test("DIFFERENT RELATIONSHIPS ((a)-[:REL]->(b)){2}") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldBe empty
  }

  test("((a)-[:REL]->(b)){2}, ((c)-[:REL]->(d))+") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldBe empty
  }

  test("REPEATABLE ELEMENTS ((a)-[:REL]->(b)){2}") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldBe empty
  }

  test("REPEATABLE ELEMENTS ((a)-[:REL]->(b)){1,}") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldEqual Seq(
      semanticErrorWithPos(InputPosition(26, 1, 27))
    )
  }

  test("REPEATABLE ELEMENTS ((a)-[:REL]->(b))+") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldEqual Seq(
      semanticErrorWithPos(InputPosition(26, 1, 27))
    )
  }

  test("REPEATABLE ELEMENTS (a)-[:REL*]->(b)") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldEqual Seq(
      semanticErrorWithPos(InputPosition(26, 1, 27))
    )
  }

  test("REPEATABLE ELEMENTS SHORTEST 2 PATH ((a)-[:REL]->(b))+") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldBe empty
  }

  test("REPEATABLE ELEMENTS ANY ((a)-[:REL]->(b))+") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldBe empty
  }

  test("REPEATABLE ELEMENTS SHORTEST 1 PATH GROUPS ((a)-[:REL]->(b))+") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldBe empty
  }

  test("REPEATABLE ELEMENTS p = shortestPath((a)-[:REL*]->(b))") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldBe empty
  }

  test("REPEATABLE ELEMENTS ((a)-[:REL]->(b)){2}, ((c)-[:REL]->(d))+") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldEqual Seq(
      semanticErrorWithPos(InputPosition(48, 1, 49))
    )
  }

  test("REPEATABLE ELEMENTS ((a)-[:REL]->(b))+, ((c)-[:REL]->(d))+") {
    runSemanticAnalysisWithSemanticFeatures(semanticFeatures: _*).errors shouldEqual Seq(
      semanticErrorWithPos(InputPosition(26, 1, 27)),
      semanticErrorWithPos(InputPosition(46, 1, 47))
    )
  }
}