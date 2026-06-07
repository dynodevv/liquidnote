package com.liquidnote.app

import android.app.Application
import androidx.room.Room
import com.liquidnote.app.data.AppDatabase
import com.liquidnote.app.repository.NoteRepository
import com.liquidnote.app.util.SettingsManager

class LiquidNoteApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: NoteRepository
        private set

    lateinit var settingsManager: SettingsManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "liquidnote.db"
        ).build()
        repository = NoteRepository(database)
        settingsManager = SettingsManager(applicationContext)
    }

    companion object {
        fun instance(app: Application): LiquidNoteApplication = app as LiquidNoteApplication
    }
}
