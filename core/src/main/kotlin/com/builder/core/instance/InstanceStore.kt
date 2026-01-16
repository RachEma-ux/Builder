package com.builder.core.instance

/**
 * Persistence contract used by runtime.
 * This MUST NOT depend on Room or DAO types.
 *
 * NOTE:
 * - Keep these methods minimal.
 * - If compilation fails after this script, adjust the method signatures to match what InstanceManager actually needs.
 */
interface InstanceStore {
  /**
   * Temporary minimal surface.
   * Replace these with real domain-friendly methods once you check InstanceManager usage.
   */
  fun listIds(): List<String>
  fun saveId(id: String)
}
