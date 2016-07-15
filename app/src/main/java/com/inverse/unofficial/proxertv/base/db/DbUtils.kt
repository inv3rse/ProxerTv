package com.inverse.unofficial.proxertv.base.db

import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import rx.Observable

fun <T> ManagedSQLiteOpenHelper.useAsync(f: SQLiteDatabase.() -> T): Observable<T> {
    return Observable.fromCallable {
        this.use(f)
    }
}