package com.builder.data.local.instance

import com.builder.core.instance.InstanceStore
import com.builder.data.local.db.dao.InstanceDao
import javax.inject.Inject

/**
 * Room-backed implementation living in :data.
 * Adjust mapping to match your InstanceDao methods and entity model.
 */
class RoomInstanceStore @Inject constructor(
  private val dao: InstanceDao
) : InstanceStore {

  override fun listIds(): List<String> {
    // TODO: map to your real DAO method(s).
    // Example (you MUST adapt):
    // return dao.getAll().map { it.id }
    return emptyList()
  }

  override fun saveId(id: String) {
    // TODO: map to your real DAO insert'ish method(s).
    // Example (you MUST adapt):
    // dao.insert(InstanceEntity(id = id))
  }
}
