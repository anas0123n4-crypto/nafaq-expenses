package com.nafaq.expenses.data

import android.content.Context
import com.nafaq.expenses.data.ai.AiManager
import com.nafaq.expenses.data.parser.ExpenseParser
import com.nafaq.expenses.data.parser.LexiconMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database by lazy { com.nafaq.expenses.data.db.NafaqDatabase.build(appContext) }

    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    val lexicon: LexiconRepository by lazy { LexiconRepository(appContext) }

    val matcher: LexiconMatcher by lazy { LexiconMatcher(lexicon.entries) }

    val parser: ExpenseParser by lazy { ExpenseParser(matcher) }

    val repository: ExpenseRepository by lazy {
        ExpenseRepository(database.expenseDao(), database.categoryDao())
    }

    val ai: AiManager by lazy { AiManager(settings) }

    fun start() {
        scope.launch { repository.seedCategoriesIfEmpty() }
    }
}
