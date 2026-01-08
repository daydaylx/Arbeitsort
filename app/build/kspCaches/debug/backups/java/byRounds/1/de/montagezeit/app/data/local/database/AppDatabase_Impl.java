package de.montagezeit.app.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import de.montagezeit.app.data.local.dao.WorkEntryDao;
import de.montagezeit.app.data.local.dao.WorkEntryDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile WorkEntryDao _workEntryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `work_entries` (`date` TEXT NOT NULL, `workStart` TEXT NOT NULL, `workEnd` TEXT NOT NULL, `breakMinutes` INTEGER NOT NULL, `dayType` TEXT NOT NULL, `morningCapturedAt` INTEGER, `morningLocationLabel` TEXT, `morningLat` REAL, `morningLon` REAL, `morningAccuracyMeters` REAL, `outsideLeipzigMorning` INTEGER, `morningLocationStatus` TEXT NOT NULL, `eveningCapturedAt` INTEGER, `eveningLocationLabel` TEXT, `eveningLat` REAL, `eveningLon` REAL, `eveningAccuracyMeters` REAL, `outsideLeipzigEvening` INTEGER, `eveningLocationStatus` TEXT NOT NULL, `travelStartAt` INTEGER, `travelArriveAt` INTEGER, `travelLabelStart` TEXT, `travelLabelEnd` TEXT, `needsReview` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`date`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7c1c4a36e7868ffb2b236afdca35cda8')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `work_entries`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsWorkEntries = new HashMap<String, TableInfo.Column>(27);
        _columnsWorkEntries.put("date", new TableInfo.Column("date", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("workStart", new TableInfo.Column("workStart", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("workEnd", new TableInfo.Column("workEnd", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("breakMinutes", new TableInfo.Column("breakMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("dayType", new TableInfo.Column("dayType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("morningCapturedAt", new TableInfo.Column("morningCapturedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("morningLocationLabel", new TableInfo.Column("morningLocationLabel", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("morningLat", new TableInfo.Column("morningLat", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("morningLon", new TableInfo.Column("morningLon", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("morningAccuracyMeters", new TableInfo.Column("morningAccuracyMeters", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("outsideLeipzigMorning", new TableInfo.Column("outsideLeipzigMorning", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("morningLocationStatus", new TableInfo.Column("morningLocationStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("eveningCapturedAt", new TableInfo.Column("eveningCapturedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("eveningLocationLabel", new TableInfo.Column("eveningLocationLabel", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("eveningLat", new TableInfo.Column("eveningLat", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("eveningLon", new TableInfo.Column("eveningLon", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("eveningAccuracyMeters", new TableInfo.Column("eveningAccuracyMeters", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("outsideLeipzigEvening", new TableInfo.Column("outsideLeipzigEvening", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("eveningLocationStatus", new TableInfo.Column("eveningLocationStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("travelStartAt", new TableInfo.Column("travelStartAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("travelArriveAt", new TableInfo.Column("travelArriveAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("travelLabelStart", new TableInfo.Column("travelLabelStart", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("travelLabelEnd", new TableInfo.Column("travelLabelEnd", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("needsReview", new TableInfo.Column("needsReview", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkEntries.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWorkEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWorkEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWorkEntries = new TableInfo("work_entries", _columnsWorkEntries, _foreignKeysWorkEntries, _indicesWorkEntries);
        final TableInfo _existingWorkEntries = TableInfo.read(db, "work_entries");
        if (!_infoWorkEntries.equals(_existingWorkEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "work_entries(de.montagezeit.app.data.local.entity.WorkEntry).\n"
                  + " Expected:\n" + _infoWorkEntries + "\n"
                  + " Found:\n" + _existingWorkEntries);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "7c1c4a36e7868ffb2b236afdca35cda8", "a779b989a80208b0b33b57e60e4336a2");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "work_entries");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `work_entries`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(WorkEntryDao.class, WorkEntryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public WorkEntryDao workEntryDao() {
    if (_workEntryDao != null) {
      return _workEntryDao;
    } else {
      synchronized(this) {
        if(_workEntryDao == null) {
          _workEntryDao = new WorkEntryDao_Impl(this);
        }
        return _workEntryDao;
      }
    }
  }
}
