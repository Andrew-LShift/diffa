/**
 * Copyright (C) 2010-2012 LShift Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lshift.diffa.kernel.config

import net.lshift.diffa.schema.servicelimits._
import net.lshift.diffa.schema.jooq.{DatabaseFacade => JooqDatabaseFacade}
import net.lshift.diffa.schema.tables.PairLimits.PAIR_LIMITS
import net.lshift.diffa.schema.tables.DomainLimits.DOMAIN_LIMITS
import net.lshift.diffa.schema.tables.SystemLimits.SYSTEM_LIMITS
import net.lshift.diffa.schema.tables.LimitDefinitions.LIMIT_DEFINITIONS
import org.jooq.impl.{TableImpl, Factory}
import org.jooq.{Condition, TableField}
import scala.collection.JavaConversions._
import net.lshift.diffa.schema.tables.records.{DomainLimitsRecord, SystemLimitsRecord}

class JooqServiceLimitsStore(jooq:JooqDatabaseFacade) extends ServiceLimitsStore {

  private def validate(limitValue: Int) {
    if (limitValue < 0 && limitValue != Unlimited.hardLimit)
      throw new Exception("Invalid limit value")
  }

  def defineLimit(limit: ServiceLimit) = jooq.execute(t => {
    t.insertInto(LIMIT_DEFINITIONS).
      set(LIMIT_DEFINITIONS.NAME, limit.key).
      set(LIMIT_DEFINITIONS.DESCRIPTION, limit.description).
    onDuplicateKeyUpdate().
      set(LIMIT_DEFINITIONS.DESCRIPTION, limit.description).
    execute()
  })

  def deleteDomainLimits(domain: String) = jooq.execute(t => {

    deletePairLimitsByDomainInternal(t, domain)

    t.delete(DOMAIN_LIMITS).
      where(DOMAIN_LIMITS.DOMAIN.equal(domain)).
      execute()
  })

  def deletePairLimitsByDomain(domain: String) = jooq.execute(deletePairLimitsByDomainInternal(_, domain))

  def setSystemHardLimit(limit: ServiceLimit, limitValue: Int) = jooq.execute(t => {
    validate(limitValue)

    setSystemLimit(t, limit, limitValue, SYSTEM_LIMITS.HARD_LIMIT)
    cascadeToDomainLimits(t, limit, limitValue, DOMAIN_LIMITS.HARD_LIMIT)
  })

  def setDomainHardLimit(domainName: String, limit: ServiceLimit, limitValue: Int) = jooq.execute(t => {
    validate(limitValue)

    setDomainLimit(t, domainName, limit, limitValue, DOMAIN_LIMITS.HARD_LIMIT)
  })

  def setSystemDefaultLimit(limit: ServiceLimit, limitValue: Int) = jooq.execute(t => {
    setSystemLimit(t, limit, limitValue, SYSTEM_LIMITS.DEFAULT_LIMIT)
  })

  def setDomainDefaultLimit(domainName: String, limit: ServiceLimit, limitValue: Int) = jooq.execute(t => {
    setDomainLimit(t, domainName, limit, limitValue, DOMAIN_LIMITS.DEFAULT_LIMIT)
  })

  def setPairLimit(domainName: String, pairKey: String, limit: ServiceLimit, limitValue: Int) =
    jooq.execute(setPairLimit(_, domainName, pairKey, limit, limitValue))

  def getSystemHardLimitForName(limit: ServiceLimit) =
    getLimit(SYSTEM_LIMITS.HARD_LIMIT, SYSTEM_LIMITS, SYSTEM_LIMITS.NAME.equal(limit.key))

  def getSystemDefaultLimitForName(limit: ServiceLimit) =
    getLimit(SYSTEM_LIMITS.DEFAULT_LIMIT, SYSTEM_LIMITS, SYSTEM_LIMITS.NAME.equal(limit.key))

  def getDomainHardLimitForDomainAndName(domainName: String, limit: ServiceLimit) =
    getLimit(DOMAIN_LIMITS.HARD_LIMIT, DOMAIN_LIMITS, DOMAIN_LIMITS.NAME.equal(limit.key), DOMAIN_LIMITS.DOMAIN.equal(domainName))

  def getDomainDefaultLimitForDomainAndName(domainName: String, limit: ServiceLimit) =
    getLimit(DOMAIN_LIMITS.DEFAULT_LIMIT, DOMAIN_LIMITS, DOMAIN_LIMITS.NAME.equal(limit.key), DOMAIN_LIMITS.DOMAIN.equal(domainName))

  def getPairLimitForPairAndName(domainName: String, pairKey: String, limit: ServiceLimit) =
    getLimit(PAIR_LIMITS.LIMIT_VALUE, PAIR_LIMITS,
             PAIR_LIMITS.NAME.equal(limit.key), PAIR_LIMITS.DOMAIN.equal(domainName), PAIR_LIMITS.PAIR_KEY.equal(pairKey))

  private def getLimit(limitValue:TableField[_,_], table:TableImpl[_], predicate:Condition*) : Option[Int] = jooq.execute(t => {

    val record = t.select(limitValue).from(table).where(predicate).fetchOne()

    if (record != null) {
      Some(record.getValueAsInteger(limitValue))
    }
    else {
      None
    }
  })

  private def setPairLimit(t: Factory, domainName: String, pairKey: String, limit: ServiceLimit, limitValue: Int) = {
    t.insertInto(PAIR_LIMITS).
        set(PAIR_LIMITS.NAME, limit.key).
        set(PAIR_LIMITS.LIMIT_VALUE, int2Integer(limitValue)).
        set(PAIR_LIMITS.DOMAIN, domainName).
        set(PAIR_LIMITS.PAIR_KEY, pairKey).
      onDuplicateKeyUpdate().
        set(PAIR_LIMITS.LIMIT_VALUE, int2Integer(limitValue)).
      execute()
  }

  /**
   * If there isn't a row for the the particular limit, then irrespective of whether setting the hard or default limit,
   * set both columns to the same value. If the row already exists, then just update the requested column (i.e. either
   * the hard or the default limit). After having performed the UPSERT, make sure that the default limit is never greater
   * than the hard limit.
   */

  private def setSystemLimit(t:Factory, limit: ServiceLimit, limitValue: java.lang.Integer, fieldToLimit:TableField[SystemLimitsRecord,java.lang.Integer]) = {
    t.insertInto(SYSTEM_LIMITS).
        set(SYSTEM_LIMITS.NAME, limit.key).
        set(SYSTEM_LIMITS.HARD_LIMIT, limitValue).
        set(SYSTEM_LIMITS.DEFAULT_LIMIT, limitValue).
      onDuplicateKeyUpdate().
        set(fieldToLimit, limitValue).
      execute()

    verifySystemDefaultLimit(t)
  }

  private def setDomainLimit(t:Factory, domain:String, limit: ServiceLimit, limitValue: java.lang.Integer, fieldToLimit:TableField[DomainLimitsRecord,java.lang.Integer]) = {
    t.insertInto(DOMAIN_LIMITS).
        set(DOMAIN_LIMITS.DOMAIN, domain).
        set(DOMAIN_LIMITS.NAME, limit.key).
        set(DOMAIN_LIMITS.HARD_LIMIT, limitValue).
        set(DOMAIN_LIMITS.DEFAULT_LIMIT, limitValue).
      onDuplicateKeyUpdate().
        set(fieldToLimit, limitValue).
      execute()

    verifyDomainDefaultLimit(t)
    cascadeToPairLimits(t, domain, limit, limitValue)

  }

  /**
   * This will only get called within the scope of setting a system hard limit, so be careful when trying to it :-)
   */
  private def cascadeToDomainLimits(t:Factory, limit: ServiceLimit, limitValue: java.lang.Integer, fieldToLimit:TableField[DomainLimitsRecord,java.lang.Integer]) = {
    t.update(DOMAIN_LIMITS).
        set(fieldToLimit, limitValue).
      where(DOMAIN_LIMITS.NAME.equal(limit.key)).
      execute()

    verifyDomainDefaultLimit(t)
    verifyPairLimit(t)
  }

  private def cascadeToPairLimits(t:Factory, domain:String, limit: ServiceLimit, limitValue: java.lang.Integer) = {
    t.update(PAIR_LIMITS).
        set(PAIR_LIMITS.LIMIT_VALUE, limitValue).
      where(PAIR_LIMITS.NAME.equal(limit.key)).
        and(PAIR_LIMITS.DOMAIN.equal(domain)).
        and(PAIR_LIMITS.LIMIT_VALUE.greaterThan(limitValue)).
      execute()
  }

  private def verifySystemDefaultLimit(t:Factory) = {
    t.update(SYSTEM_LIMITS).
        set(SYSTEM_LIMITS.DEFAULT_LIMIT, SYSTEM_LIMITS.HARD_LIMIT).
      where(SYSTEM_LIMITS.DEFAULT_LIMIT.greaterThan(SYSTEM_LIMITS.HARD_LIMIT)).
      execute()
  }

  private def verifyDomainDefaultLimit(t:Factory) = {
    t.update(DOMAIN_LIMITS).
        set(DOMAIN_LIMITS.DEFAULT_LIMIT, DOMAIN_LIMITS.HARD_LIMIT).
      where(DOMAIN_LIMITS.DEFAULT_LIMIT.greaterThan(DOMAIN_LIMITS.HARD_LIMIT)).
      execute()
  }

  private def verifyPairLimit(t:Factory) = {
    t.update(PAIR_LIMITS).
        set(PAIR_LIMITS.LIMIT_VALUE, SYSTEM_LIMITS.HARD_LIMIT).
      where(PAIR_LIMITS.LIMIT_VALUE.greaterThan(SYSTEM_LIMITS.HARD_LIMIT)).
      execute()
  }

  private def deletePairLimitsByDomainInternal(t:Factory, domain: String) = {
    t.delete(PAIR_LIMITS).
      where(PAIR_LIMITS.DOMAIN.equal(domain)).
      execute()
  }
}