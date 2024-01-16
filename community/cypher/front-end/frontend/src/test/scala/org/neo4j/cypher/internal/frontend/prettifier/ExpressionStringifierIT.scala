/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
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
package org.neo4j.cypher.internal.frontend.prettifier

import org.neo4j.cypher.internal.ast.factory.neo4j.JavaccRule
import org.neo4j.cypher.internal.ast.prettifier.ExpressionStringifier
import org.neo4j.cypher.internal.util.test_helpers.CypherFunSuite
import org.neo4j.cypher.internal.util.test_helpers.WindowsStringSafe

class ExpressionStringifierIT extends CypherFunSuite {

  implicit val windowsSafe: WindowsStringSafe.type = WindowsStringSafe

  val stringifier: ExpressionStringifier = ExpressionStringifier()

  val tests: Seq[(String, String)] =
    Seq[(String, String)](
      "42" -> "42",
      "[1,2,3,4]" -> "[1, 2, 3, 4]",
      "1+2" -> "1 + 2",
      "(1)+2" -> "1 + 2",
      "(1+2)*3" -> "(1 + 2) * 3",
      "1+2*3" -> "1 + 2 * 3",
      "collect(n)[3]" -> "collect(n)[3]",
      "collect(n)[3..4]" -> "collect(n)[3..4]",
      "collect(n)[2..]" -> "collect(n)[2..]",
      "collect(n)[..2]" -> "collect(n)[..2]",
      "[x in [1,2,3] | x * 2]" -> "[x IN [1, 2, 3] | x * 2]",
      "[x in [1,2,3]\n\tWHERE x%2=0|x*2]" -> "[x IN [1, 2, 3] WHERE x % 2 = 0 | x * 2]",
      "[x in [1,2,3]\n\tWHERE x%2=0]" -> "[x IN [1, 2, 3] WHERE x % 2 = 0]",
      "[(a)-->(b)|a.prop]" -> "[(a)-->(b) | a.prop]",
      "[p=(a)-->(b) WHERE a:APA|a.prop*length(p)]" -> "[p = (a)-->(b) WHERE a:APA | a.prop * length(p)]",
      "n['apa']" -> "n[\"apa\"]",
      "'apa'" -> "\"apa\"",
      "'a\"pa'" -> "'a\"pa'",
      "\"a'pa\"" -> "\"a'pa\"",
      "\"a'\\\"pa\"" -> "\"a'\\\"pa\"",
      "any(x in ['a','b', 'c'] where x > 28)" -> "any(x IN [\"a\", \"b\", \"c\"] WHERE x > 28)",
      "all(x in ['a','b', 'c'] where x > 28)" -> "all(x IN [\"a\", \"b\", \"c\"] WHERE x > 28)",
      "none(x in ['a','b', 'c'] where x > 28)" -> "none(x IN [\"a\", \"b\", \"c\"] WHERE x > 28)",
      "{k: 'apa', id: 42}" -> "{k: \"apa\", id: 42}",
      "()<--()-->()" -> "()<--()-->()",
      "()<-[*]-()" -> "()<-[*]-()",
      "()<-[*1..]-()" -> "()<-[*1..]-()",
      "()<-[*..2]-()" -> "()<-[*..2]-()",
      "()<-[*2..4]-()" -> "()<-[*2..4]-()",
      "(:Label)<-[var]-({id:43})-->(v:X)" -> "(:Label)<-[var]-({id: 43})-->(v:X)",
      "n{.*,.bar,baz:42,variable}" -> "n{.*, .bar, baz: 42, variable}",
      "n:A:B" -> "n:A:B",
      "not(true)" -> "NOT true",
      "case when 1 = n.prop then 1 when 2 = n.prop then 2 else 4 end" ->
        "CASE WHEN 1 = n.prop THEN 1 WHEN 2 = n.prop THEN 2 ELSE 4 END",
      "case n.prop when 1 then '1' when 2 then '2' else '4' end" ->
        "CASE n.prop WHEN 1 THEN \"1\" WHEN 2 THEN \"2\" ELSE \"4\" END",
      "not(((1) = (2)) and ((3) = (4)))" -> "NOT (1 = 2 AND 3 = 4)",
      "reduce(totalAge = 0, n IN nodes(p)| totalAge + n.age)" ->
        "reduce(totalAge = 0, n IN nodes(p) | totalAge + n.age)",
      "$param1+$param2" -> "$param1 + $param2",
      "(:Label)--()" -> "(:Label)--()",
      "(:Label {prop:1})--()" -> "(:Label {prop: 1})--()",
      "()-[:Type {prop:1}]-()" -> "()-[:Type {prop: 1}]-()",
      "EXISTS { MATCH (n)}" -> "EXISTS { MATCH (n) }",
      "EXISTS { MATCH (n) WHERE n.prop = 'f'}" -> "EXISTS { MATCH (n)\n  WHERE n.prop = \"f\" }",
      "EXISTS { MATCH (n : Label)-[:HAS_REL]->(m) WHERE n.prop = 'f'}" -> "EXISTS { MATCH (n:Label)-[:HAS_REL]->(m)\n  WHERE n.prop = \"f\" }",
      "EXISTS { MATCH (n : Label)-[:HAS_REL]->(m) WHERE n.prop = 'f' RETURN n }" -> "EXISTS { MATCH (n:Label)-[:HAS_REL]->(m)\n  WHERE n.prop = \"f\"\nRETURN n }",
      "COUNT {(n)}" -> "COUNT { MATCH (n) }",
      "COUNT {(n)<-[]->(m)}" -> "COUNT { MATCH (n)--(m) }",
      "COUNT {(n : Label)-[:HAS_REL]->(m) WHERE n.prop = 'f'}" -> "COUNT { MATCH (n:Label)-[:HAS_REL]->(m)\n  WHERE n.prop = \"f\" }",
      "COUNT { MATCH (n : Label)-[:HAS_REL]->(m) WHERE n.prop = 'f' RETURN n }" -> "COUNT { MATCH (n:Label)-[:HAS_REL]->(m)\n  WHERE n.prop = \"f\"\nRETURN n }",
      "reduce(totalAge = 0, n IN nodes(p)| totalAge + n.age) + 4 * 5" -> "reduce(totalAge = 0, n IN nodes(p) | totalAge + n.age) + 4 * 5",
      "1 < 2 > 3 = 4 >= 5 <= 6" -> "1 < 2 > 3 = 4 >= 5 <= 6",
      "1 < 2 > 3 = 4 >= 5 <= 6 AND a OR b" -> "(1 < 2 > 3 = 4 >= 5 <= 6) AND a OR b",
      "x IS TYPED nothing" -> "x IS :: NOTHING",
      "x IS TYPED null" -> "x IS :: NULL",
      "x IS TYPED bool" -> "x IS :: BOOLEAN",
      "n.prop is :: varChar" -> "n.prop IS :: STRING",
      "1 :: InT" -> "1 IS :: INTEGER",
      "['2'] IS not TYPED TIMESTAMP without TIMEZONE" -> "[\"2\"] IS NOT :: LOCAL DATETIME",
      "$param is NOT :: time without TIMEZONE" -> "$param IS NOT :: LOCAL TIME",
      "1 :: SIGNED INTEGER OR 1 IS NOT TYPED point" -> "1 IS :: INTEGER OR 1 IS NOT :: POINT",
      "1 :: ANY VERTEX" -> "1 IS :: NODE",
      "1 :: ANY EDGE" -> "1 IS :: RELATIONSHIP",
      "1 :: ANY MAP" -> "1 IS :: MAP",
      "1 :: path" -> "1 IS :: PATH",
      "1 is typed ANY PROPERTY VALUE" -> "1 IS :: PROPERTY VALUE",
      "1 is typed ANY VALUE" -> "1 IS :: ANY",
      "1 :: LIST < INT   >" -> "1 IS :: LIST<INTEGER>",
      "1 :: ARRAY <  VARcHAr  >" -> "1 IS :: LIST<STRING>",
      "1 :: any value    <  int    | bool   | bool  >" -> "1 IS :: BOOLEAN | INTEGER",
      "1 :: any     <  int    | bool   | bool  >" -> "1 IS :: BOOLEAN | INTEGER",
      "1 ::  int    | bool   | bool  " -> "1 IS :: BOOLEAN | INTEGER",
      "x IS nfc NORMALIZED" -> "x IS NFC NORMALIZED",
      "x IS not normalized" -> "x IS NOT NFC NORMALIZED"
    )

  tests foreach {
    case (inputString, expected) =>
      test(inputString) {
        val expression = JavaccRule.Expression.apply(inputString)
        val str = stringifier(expression)
        str should equal(expected)
      }
  }

}
