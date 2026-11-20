package com.example.loantracker.data.local.repository


import com.example.loantracker.data.local.daos.NoteDao
import com.example.loantracker.data.local.entities.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NoteRepository @Inject constructor(private val dao: NoteDao){
    val notes = dao.getNotes()

    suspend fun insert(note: NoteEntity){
        dao.insertNote(note)
    }

    suspend fun update(note: NoteEntity){
        dao.updateNote(note)
    }

    suspend fun delete(note: NoteEntity){
        dao.deleteNote((note))
    }

    fun getByProjectId(projectId: Long): Flow<List<NoteEntity>> {
        return dao.getNotesByProjectId(projectId)
    }
}