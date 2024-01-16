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
package org.neo4j.cypher.internal.expressions

sealed trait NormalForm {
  protected def formName: String
  def description: String = s"$formName"
  override def toString: String = description
}

case object NFCNormalForm extends NormalForm {
  protected val formName: String = "NFC"
}

case object NFDNormalForm extends NormalForm {
  protected val formName: String = "NFD"
}

case object NFKCNormalForm extends NormalForm {
  protected val formName: String = "NFKC"
}

case object NFKDNormalForm extends NormalForm {
  protected val formName: String = "NFKD"
}
