package de.montagezeit.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import de.montagezeit.app.data.local.converters.LocalDateConverter;
import de.montagezeit.app.data.local.converters.LocalTimeConverter;
import de.montagezeit.app.data.local.entity.DayType;
import de.montagezeit.app.data.local.entity.LocationStatus;
import de.montagezeit.app.data.local.entity.WorkEntry;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Float;
import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WorkEntryDao_Impl implements WorkEntryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WorkEntry> __insertionAdapterOfWorkEntry;

  private final LocalDateConverter __localDateConverter = new LocalDateConverter();

  private final LocalTimeConverter __localTimeConverter = new LocalTimeConverter();

  private final EntityDeletionOrUpdateAdapter<WorkEntry> __updateAdapterOfWorkEntry;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByDate;

  public WorkEntryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWorkEntry = new EntityInsertionAdapter<WorkEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `work_entries` (`date`,`workStart`,`workEnd`,`breakMinutes`,`dayType`,`morningCapturedAt`,`morningLocationLabel`,`morningLat`,`morningLon`,`morningAccuracyMeters`,`outsideLeipzigMorning`,`morningLocationStatus`,`eveningCapturedAt`,`eveningLocationLabel`,`eveningLat`,`eveningLon`,`eveningAccuracyMeters`,`outsideLeipzigEvening`,`eveningLocationStatus`,`travelStartAt`,`travelArriveAt`,`travelLabelStart`,`travelLabelEnd`,`needsReview`,`note`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkEntry entity) {
        final String _tmp = __localDateConverter.fromLocalDate(entity.getDate());
        if (_tmp == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, _tmp);
        }
        final String _tmp_1 = __localTimeConverter.fromLocalTime(entity.getWorkStart());
        if (_tmp_1 == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, _tmp_1);
        }
        final String _tmp_2 = __localTimeConverter.fromLocalTime(entity.getWorkEnd());
        if (_tmp_2 == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, _tmp_2);
        }
        statement.bindLong(4, entity.getBreakMinutes());
        statement.bindString(5, __DayType_enumToString(entity.getDayType()));
        if (entity.getMorningCapturedAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getMorningCapturedAt());
        }
        if (entity.getMorningLocationLabel() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getMorningLocationLabel());
        }
        if (entity.getMorningLat() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getMorningLat());
        }
        if (entity.getMorningLon() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getMorningLon());
        }
        if (entity.getMorningAccuracyMeters() == null) {
          statement.bindNull(10);
        } else {
          statement.bindDouble(10, entity.getMorningAccuracyMeters());
        }
        final Integer _tmp_3 = entity.getOutsideLeipzigMorning() == null ? null : (entity.getOutsideLeipzigMorning() ? 1 : 0);
        if (_tmp_3 == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, _tmp_3);
        }
        statement.bindString(12, __LocationStatus_enumToString(entity.getMorningLocationStatus()));
        if (entity.getEveningCapturedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getEveningCapturedAt());
        }
        if (entity.getEveningLocationLabel() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getEveningLocationLabel());
        }
        if (entity.getEveningLat() == null) {
          statement.bindNull(15);
        } else {
          statement.bindDouble(15, entity.getEveningLat());
        }
        if (entity.getEveningLon() == null) {
          statement.bindNull(16);
        } else {
          statement.bindDouble(16, entity.getEveningLon());
        }
        if (entity.getEveningAccuracyMeters() == null) {
          statement.bindNull(17);
        } else {
          statement.bindDouble(17, entity.getEveningAccuracyMeters());
        }
        final Integer _tmp_4 = entity.getOutsideLeipzigEvening() == null ? null : (entity.getOutsideLeipzigEvening() ? 1 : 0);
        if (_tmp_4 == null) {
          statement.bindNull(18);
        } else {
          statement.bindLong(18, _tmp_4);
        }
        statement.bindString(19, __LocationStatus_enumToString(entity.getEveningLocationStatus()));
        if (entity.getTravelStartAt() == null) {
          statement.bindNull(20);
        } else {
          statement.bindLong(20, entity.getTravelStartAt());
        }
        if (entity.getTravelArriveAt() == null) {
          statement.bindNull(21);
        } else {
          statement.bindLong(21, entity.getTravelArriveAt());
        }
        if (entity.getTravelLabelStart() == null) {
          statement.bindNull(22);
        } else {
          statement.bindString(22, entity.getTravelLabelStart());
        }
        if (entity.getTravelLabelEnd() == null) {
          statement.bindNull(23);
        } else {
          statement.bindString(23, entity.getTravelLabelEnd());
        }
        final int _tmp_5 = entity.getNeedsReview() ? 1 : 0;
        statement.bindLong(24, _tmp_5);
        if (entity.getNote() == null) {
          statement.bindNull(25);
        } else {
          statement.bindString(25, entity.getNote());
        }
        statement.bindLong(26, entity.getCreatedAt());
        statement.bindLong(27, entity.getUpdatedAt());
      }
    };
    this.__updateAdapterOfWorkEntry = new EntityDeletionOrUpdateAdapter<WorkEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `work_entries` SET `date` = ?,`workStart` = ?,`workEnd` = ?,`breakMinutes` = ?,`dayType` = ?,`morningCapturedAt` = ?,`morningLocationLabel` = ?,`morningLat` = ?,`morningLon` = ?,`morningAccuracyMeters` = ?,`outsideLeipzigMorning` = ?,`morningLocationStatus` = ?,`eveningCapturedAt` = ?,`eveningLocationLabel` = ?,`eveningLat` = ?,`eveningLon` = ?,`eveningAccuracyMeters` = ?,`outsideLeipzigEvening` = ?,`eveningLocationStatus` = ?,`travelStartAt` = ?,`travelArriveAt` = ?,`travelLabelStart` = ?,`travelLabelEnd` = ?,`needsReview` = ?,`note` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `date` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkEntry entity) {
        final String _tmp = __localDateConverter.fromLocalDate(entity.getDate());
        if (_tmp == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, _tmp);
        }
        final String _tmp_1 = __localTimeConverter.fromLocalTime(entity.getWorkStart());
        if (_tmp_1 == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, _tmp_1);
        }
        final String _tmp_2 = __localTimeConverter.fromLocalTime(entity.getWorkEnd());
        if (_tmp_2 == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, _tmp_2);
        }
        statement.bindLong(4, entity.getBreakMinutes());
        statement.bindString(5, __DayType_enumToString(entity.getDayType()));
        if (entity.getMorningCapturedAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getMorningCapturedAt());
        }
        if (entity.getMorningLocationLabel() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getMorningLocationLabel());
        }
        if (entity.getMorningLat() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getMorningLat());
        }
        if (entity.getMorningLon() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getMorningLon());
        }
        if (entity.getMorningAccuracyMeters() == null) {
          statement.bindNull(10);
        } else {
          statement.bindDouble(10, entity.getMorningAccuracyMeters());
        }
        final Integer _tmp_3 = entity.getOutsideLeipzigMorning() == null ? null : (entity.getOutsideLeipzigMorning() ? 1 : 0);
        if (_tmp_3 == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, _tmp_3);
        }
        statement.bindString(12, __LocationStatus_enumToString(entity.getMorningLocationStatus()));
        if (entity.getEveningCapturedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getEveningCapturedAt());
        }
        if (entity.getEveningLocationLabel() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getEveningLocationLabel());
        }
        if (entity.getEveningLat() == null) {
          statement.bindNull(15);
        } else {
          statement.bindDouble(15, entity.getEveningLat());
        }
        if (entity.getEveningLon() == null) {
          statement.bindNull(16);
        } else {
          statement.bindDouble(16, entity.getEveningLon());
        }
        if (entity.getEveningAccuracyMeters() == null) {
          statement.bindNull(17);
        } else {
          statement.bindDouble(17, entity.getEveningAccuracyMeters());
        }
        final Integer _tmp_4 = entity.getOutsideLeipzigEvening() == null ? null : (entity.getOutsideLeipzigEvening() ? 1 : 0);
        if (_tmp_4 == null) {
          statement.bindNull(18);
        } else {
          statement.bindLong(18, _tmp_4);
        }
        statement.bindString(19, __LocationStatus_enumToString(entity.getEveningLocationStatus()));
        if (entity.getTravelStartAt() == null) {
          statement.bindNull(20);
        } else {
          statement.bindLong(20, entity.getTravelStartAt());
        }
        if (entity.getTravelArriveAt() == null) {
          statement.bindNull(21);
        } else {
          statement.bindLong(21, entity.getTravelArriveAt());
        }
        if (entity.getTravelLabelStart() == null) {
          statement.bindNull(22);
        } else {
          statement.bindString(22, entity.getTravelLabelStart());
        }
        if (entity.getTravelLabelEnd() == null) {
          statement.bindNull(23);
        } else {
          statement.bindString(23, entity.getTravelLabelEnd());
        }
        final int _tmp_5 = entity.getNeedsReview() ? 1 : 0;
        statement.bindLong(24, _tmp_5);
        if (entity.getNote() == null) {
          statement.bindNull(25);
        } else {
          statement.bindString(25, entity.getNote());
        }
        statement.bindLong(26, entity.getCreatedAt());
        statement.bindLong(27, entity.getUpdatedAt());
        final String _tmp_6 = __localDateConverter.fromLocalDate(entity.getDate());
        if (_tmp_6 == null) {
          statement.bindNull(28);
        } else {
          statement.bindString(28, _tmp_6);
        }
      }
    };
    this.__preparedStmtOfDeleteByDate = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM work_entries WHERE date = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final WorkEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfWorkEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final WorkEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWorkEntry.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final WorkEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfWorkEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByDate(final LocalDate date, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByDate.acquire();
        int _argIndex = 1;
        final String _tmp = __localDateConverter.fromLocalDate(date);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, _tmp);
        }
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByDate.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByDate(final LocalDate date, final Continuation<? super WorkEntry> $completion) {
    final String _sql = "SELECT * FROM work_entries WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __localDateConverter.fromLocalDate(date);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WorkEntry>() {
      @Override
      @Nullable
      public WorkEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWorkStart = CursorUtil.getColumnIndexOrThrow(_cursor, "workStart");
          final int _cursorIndexOfWorkEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "workEnd");
          final int _cursorIndexOfBreakMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "breakMinutes");
          final int _cursorIndexOfDayType = CursorUtil.getColumnIndexOrThrow(_cursor, "dayType");
          final int _cursorIndexOfMorningCapturedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "morningCapturedAt");
          final int _cursorIndexOfMorningLocationLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLocationLabel");
          final int _cursorIndexOfMorningLat = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLat");
          final int _cursorIndexOfMorningLon = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLon");
          final int _cursorIndexOfMorningAccuracyMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "morningAccuracyMeters");
          final int _cursorIndexOfOutsideLeipzigMorning = CursorUtil.getColumnIndexOrThrow(_cursor, "outsideLeipzigMorning");
          final int _cursorIndexOfMorningLocationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLocationStatus");
          final int _cursorIndexOfEveningCapturedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningCapturedAt");
          final int _cursorIndexOfEveningLocationLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLocationLabel");
          final int _cursorIndexOfEveningLat = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLat");
          final int _cursorIndexOfEveningLon = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLon");
          final int _cursorIndexOfEveningAccuracyMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningAccuracyMeters");
          final int _cursorIndexOfOutsideLeipzigEvening = CursorUtil.getColumnIndexOrThrow(_cursor, "outsideLeipzigEvening");
          final int _cursorIndexOfEveningLocationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLocationStatus");
          final int _cursorIndexOfTravelStartAt = CursorUtil.getColumnIndexOrThrow(_cursor, "travelStartAt");
          final int _cursorIndexOfTravelArriveAt = CursorUtil.getColumnIndexOrThrow(_cursor, "travelArriveAt");
          final int _cursorIndexOfTravelLabelStart = CursorUtil.getColumnIndexOrThrow(_cursor, "travelLabelStart");
          final int _cursorIndexOfTravelLabelEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "travelLabelEnd");
          final int _cursorIndexOfNeedsReview = CursorUtil.getColumnIndexOrThrow(_cursor, "needsReview");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final WorkEntry _result;
          if (_cursor.moveToFirst()) {
            final LocalDate _tmpDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_2 = __localDateConverter.toLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_2;
            }
            final LocalTime _tmpWorkStart;
            final String _tmp_3;
            if (_cursor.isNull(_cursorIndexOfWorkStart)) {
              _tmp_3 = null;
            } else {
              _tmp_3 = _cursor.getString(_cursorIndexOfWorkStart);
            }
            final LocalTime _tmp_4 = __localTimeConverter.toLocalTime(_tmp_3);
            if (_tmp_4 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalTime', but it was NULL.");
            } else {
              _tmpWorkStart = _tmp_4;
            }
            final LocalTime _tmpWorkEnd;
            final String _tmp_5;
            if (_cursor.isNull(_cursorIndexOfWorkEnd)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getString(_cursorIndexOfWorkEnd);
            }
            final LocalTime _tmp_6 = __localTimeConverter.toLocalTime(_tmp_5);
            if (_tmp_6 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalTime', but it was NULL.");
            } else {
              _tmpWorkEnd = _tmp_6;
            }
            final int _tmpBreakMinutes;
            _tmpBreakMinutes = _cursor.getInt(_cursorIndexOfBreakMinutes);
            final DayType _tmpDayType;
            _tmpDayType = __DayType_stringToEnum(_cursor.getString(_cursorIndexOfDayType));
            final Long _tmpMorningCapturedAt;
            if (_cursor.isNull(_cursorIndexOfMorningCapturedAt)) {
              _tmpMorningCapturedAt = null;
            } else {
              _tmpMorningCapturedAt = _cursor.getLong(_cursorIndexOfMorningCapturedAt);
            }
            final String _tmpMorningLocationLabel;
            if (_cursor.isNull(_cursorIndexOfMorningLocationLabel)) {
              _tmpMorningLocationLabel = null;
            } else {
              _tmpMorningLocationLabel = _cursor.getString(_cursorIndexOfMorningLocationLabel);
            }
            final Double _tmpMorningLat;
            if (_cursor.isNull(_cursorIndexOfMorningLat)) {
              _tmpMorningLat = null;
            } else {
              _tmpMorningLat = _cursor.getDouble(_cursorIndexOfMorningLat);
            }
            final Double _tmpMorningLon;
            if (_cursor.isNull(_cursorIndexOfMorningLon)) {
              _tmpMorningLon = null;
            } else {
              _tmpMorningLon = _cursor.getDouble(_cursorIndexOfMorningLon);
            }
            final Float _tmpMorningAccuracyMeters;
            if (_cursor.isNull(_cursorIndexOfMorningAccuracyMeters)) {
              _tmpMorningAccuracyMeters = null;
            } else {
              _tmpMorningAccuracyMeters = _cursor.getFloat(_cursorIndexOfMorningAccuracyMeters);
            }
            final Boolean _tmpOutsideLeipzigMorning;
            final Integer _tmp_7;
            if (_cursor.isNull(_cursorIndexOfOutsideLeipzigMorning)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getInt(_cursorIndexOfOutsideLeipzigMorning);
            }
            _tmpOutsideLeipzigMorning = _tmp_7 == null ? null : _tmp_7 != 0;
            final LocationStatus _tmpMorningLocationStatus;
            _tmpMorningLocationStatus = __LocationStatus_stringToEnum(_cursor.getString(_cursorIndexOfMorningLocationStatus));
            final Long _tmpEveningCapturedAt;
            if (_cursor.isNull(_cursorIndexOfEveningCapturedAt)) {
              _tmpEveningCapturedAt = null;
            } else {
              _tmpEveningCapturedAt = _cursor.getLong(_cursorIndexOfEveningCapturedAt);
            }
            final String _tmpEveningLocationLabel;
            if (_cursor.isNull(_cursorIndexOfEveningLocationLabel)) {
              _tmpEveningLocationLabel = null;
            } else {
              _tmpEveningLocationLabel = _cursor.getString(_cursorIndexOfEveningLocationLabel);
            }
            final Double _tmpEveningLat;
            if (_cursor.isNull(_cursorIndexOfEveningLat)) {
              _tmpEveningLat = null;
            } else {
              _tmpEveningLat = _cursor.getDouble(_cursorIndexOfEveningLat);
            }
            final Double _tmpEveningLon;
            if (_cursor.isNull(_cursorIndexOfEveningLon)) {
              _tmpEveningLon = null;
            } else {
              _tmpEveningLon = _cursor.getDouble(_cursorIndexOfEveningLon);
            }
            final Float _tmpEveningAccuracyMeters;
            if (_cursor.isNull(_cursorIndexOfEveningAccuracyMeters)) {
              _tmpEveningAccuracyMeters = null;
            } else {
              _tmpEveningAccuracyMeters = _cursor.getFloat(_cursorIndexOfEveningAccuracyMeters);
            }
            final Boolean _tmpOutsideLeipzigEvening;
            final Integer _tmp_8;
            if (_cursor.isNull(_cursorIndexOfOutsideLeipzigEvening)) {
              _tmp_8 = null;
            } else {
              _tmp_8 = _cursor.getInt(_cursorIndexOfOutsideLeipzigEvening);
            }
            _tmpOutsideLeipzigEvening = _tmp_8 == null ? null : _tmp_8 != 0;
            final LocationStatus _tmpEveningLocationStatus;
            _tmpEveningLocationStatus = __LocationStatus_stringToEnum(_cursor.getString(_cursorIndexOfEveningLocationStatus));
            final Long _tmpTravelStartAt;
            if (_cursor.isNull(_cursorIndexOfTravelStartAt)) {
              _tmpTravelStartAt = null;
            } else {
              _tmpTravelStartAt = _cursor.getLong(_cursorIndexOfTravelStartAt);
            }
            final Long _tmpTravelArriveAt;
            if (_cursor.isNull(_cursorIndexOfTravelArriveAt)) {
              _tmpTravelArriveAt = null;
            } else {
              _tmpTravelArriveAt = _cursor.getLong(_cursorIndexOfTravelArriveAt);
            }
            final String _tmpTravelLabelStart;
            if (_cursor.isNull(_cursorIndexOfTravelLabelStart)) {
              _tmpTravelLabelStart = null;
            } else {
              _tmpTravelLabelStart = _cursor.getString(_cursorIndexOfTravelLabelStart);
            }
            final String _tmpTravelLabelEnd;
            if (_cursor.isNull(_cursorIndexOfTravelLabelEnd)) {
              _tmpTravelLabelEnd = null;
            } else {
              _tmpTravelLabelEnd = _cursor.getString(_cursorIndexOfTravelLabelEnd);
            }
            final boolean _tmpNeedsReview;
            final int _tmp_9;
            _tmp_9 = _cursor.getInt(_cursorIndexOfNeedsReview);
            _tmpNeedsReview = _tmp_9 != 0;
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new WorkEntry(_tmpDate,_tmpWorkStart,_tmpWorkEnd,_tmpBreakMinutes,_tmpDayType,_tmpMorningCapturedAt,_tmpMorningLocationLabel,_tmpMorningLat,_tmpMorningLon,_tmpMorningAccuracyMeters,_tmpOutsideLeipzigMorning,_tmpMorningLocationStatus,_tmpEveningCapturedAt,_tmpEveningLocationLabel,_tmpEveningLat,_tmpEveningLon,_tmpEveningAccuracyMeters,_tmpOutsideLeipzigEvening,_tmpEveningLocationStatus,_tmpTravelStartAt,_tmpTravelArriveAt,_tmpTravelLabelStart,_tmpTravelLabelEnd,_tmpNeedsReview,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<WorkEntry> getByDateFlow(final LocalDate date) {
    final String _sql = "SELECT * FROM work_entries WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __localDateConverter.fromLocalDate(date);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"work_entries"}, new Callable<WorkEntry>() {
      @Override
      @Nullable
      public WorkEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWorkStart = CursorUtil.getColumnIndexOrThrow(_cursor, "workStart");
          final int _cursorIndexOfWorkEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "workEnd");
          final int _cursorIndexOfBreakMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "breakMinutes");
          final int _cursorIndexOfDayType = CursorUtil.getColumnIndexOrThrow(_cursor, "dayType");
          final int _cursorIndexOfMorningCapturedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "morningCapturedAt");
          final int _cursorIndexOfMorningLocationLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLocationLabel");
          final int _cursorIndexOfMorningLat = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLat");
          final int _cursorIndexOfMorningLon = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLon");
          final int _cursorIndexOfMorningAccuracyMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "morningAccuracyMeters");
          final int _cursorIndexOfOutsideLeipzigMorning = CursorUtil.getColumnIndexOrThrow(_cursor, "outsideLeipzigMorning");
          final int _cursorIndexOfMorningLocationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLocationStatus");
          final int _cursorIndexOfEveningCapturedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningCapturedAt");
          final int _cursorIndexOfEveningLocationLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLocationLabel");
          final int _cursorIndexOfEveningLat = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLat");
          final int _cursorIndexOfEveningLon = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLon");
          final int _cursorIndexOfEveningAccuracyMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningAccuracyMeters");
          final int _cursorIndexOfOutsideLeipzigEvening = CursorUtil.getColumnIndexOrThrow(_cursor, "outsideLeipzigEvening");
          final int _cursorIndexOfEveningLocationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLocationStatus");
          final int _cursorIndexOfTravelStartAt = CursorUtil.getColumnIndexOrThrow(_cursor, "travelStartAt");
          final int _cursorIndexOfTravelArriveAt = CursorUtil.getColumnIndexOrThrow(_cursor, "travelArriveAt");
          final int _cursorIndexOfTravelLabelStart = CursorUtil.getColumnIndexOrThrow(_cursor, "travelLabelStart");
          final int _cursorIndexOfTravelLabelEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "travelLabelEnd");
          final int _cursorIndexOfNeedsReview = CursorUtil.getColumnIndexOrThrow(_cursor, "needsReview");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final WorkEntry _result;
          if (_cursor.moveToFirst()) {
            final LocalDate _tmpDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_2 = __localDateConverter.toLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_2;
            }
            final LocalTime _tmpWorkStart;
            final String _tmp_3;
            if (_cursor.isNull(_cursorIndexOfWorkStart)) {
              _tmp_3 = null;
            } else {
              _tmp_3 = _cursor.getString(_cursorIndexOfWorkStart);
            }
            final LocalTime _tmp_4 = __localTimeConverter.toLocalTime(_tmp_3);
            if (_tmp_4 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalTime', but it was NULL.");
            } else {
              _tmpWorkStart = _tmp_4;
            }
            final LocalTime _tmpWorkEnd;
            final String _tmp_5;
            if (_cursor.isNull(_cursorIndexOfWorkEnd)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getString(_cursorIndexOfWorkEnd);
            }
            final LocalTime _tmp_6 = __localTimeConverter.toLocalTime(_tmp_5);
            if (_tmp_6 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalTime', but it was NULL.");
            } else {
              _tmpWorkEnd = _tmp_6;
            }
            final int _tmpBreakMinutes;
            _tmpBreakMinutes = _cursor.getInt(_cursorIndexOfBreakMinutes);
            final DayType _tmpDayType;
            _tmpDayType = __DayType_stringToEnum(_cursor.getString(_cursorIndexOfDayType));
            final Long _tmpMorningCapturedAt;
            if (_cursor.isNull(_cursorIndexOfMorningCapturedAt)) {
              _tmpMorningCapturedAt = null;
            } else {
              _tmpMorningCapturedAt = _cursor.getLong(_cursorIndexOfMorningCapturedAt);
            }
            final String _tmpMorningLocationLabel;
            if (_cursor.isNull(_cursorIndexOfMorningLocationLabel)) {
              _tmpMorningLocationLabel = null;
            } else {
              _tmpMorningLocationLabel = _cursor.getString(_cursorIndexOfMorningLocationLabel);
            }
            final Double _tmpMorningLat;
            if (_cursor.isNull(_cursorIndexOfMorningLat)) {
              _tmpMorningLat = null;
            } else {
              _tmpMorningLat = _cursor.getDouble(_cursorIndexOfMorningLat);
            }
            final Double _tmpMorningLon;
            if (_cursor.isNull(_cursorIndexOfMorningLon)) {
              _tmpMorningLon = null;
            } else {
              _tmpMorningLon = _cursor.getDouble(_cursorIndexOfMorningLon);
            }
            final Float _tmpMorningAccuracyMeters;
            if (_cursor.isNull(_cursorIndexOfMorningAccuracyMeters)) {
              _tmpMorningAccuracyMeters = null;
            } else {
              _tmpMorningAccuracyMeters = _cursor.getFloat(_cursorIndexOfMorningAccuracyMeters);
            }
            final Boolean _tmpOutsideLeipzigMorning;
            final Integer _tmp_7;
            if (_cursor.isNull(_cursorIndexOfOutsideLeipzigMorning)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getInt(_cursorIndexOfOutsideLeipzigMorning);
            }
            _tmpOutsideLeipzigMorning = _tmp_7 == null ? null : _tmp_7 != 0;
            final LocationStatus _tmpMorningLocationStatus;
            _tmpMorningLocationStatus = __LocationStatus_stringToEnum(_cursor.getString(_cursorIndexOfMorningLocationStatus));
            final Long _tmpEveningCapturedAt;
            if (_cursor.isNull(_cursorIndexOfEveningCapturedAt)) {
              _tmpEveningCapturedAt = null;
            } else {
              _tmpEveningCapturedAt = _cursor.getLong(_cursorIndexOfEveningCapturedAt);
            }
            final String _tmpEveningLocationLabel;
            if (_cursor.isNull(_cursorIndexOfEveningLocationLabel)) {
              _tmpEveningLocationLabel = null;
            } else {
              _tmpEveningLocationLabel = _cursor.getString(_cursorIndexOfEveningLocationLabel);
            }
            final Double _tmpEveningLat;
            if (_cursor.isNull(_cursorIndexOfEveningLat)) {
              _tmpEveningLat = null;
            } else {
              _tmpEveningLat = _cursor.getDouble(_cursorIndexOfEveningLat);
            }
            final Double _tmpEveningLon;
            if (_cursor.isNull(_cursorIndexOfEveningLon)) {
              _tmpEveningLon = null;
            } else {
              _tmpEveningLon = _cursor.getDouble(_cursorIndexOfEveningLon);
            }
            final Float _tmpEveningAccuracyMeters;
            if (_cursor.isNull(_cursorIndexOfEveningAccuracyMeters)) {
              _tmpEveningAccuracyMeters = null;
            } else {
              _tmpEveningAccuracyMeters = _cursor.getFloat(_cursorIndexOfEveningAccuracyMeters);
            }
            final Boolean _tmpOutsideLeipzigEvening;
            final Integer _tmp_8;
            if (_cursor.isNull(_cursorIndexOfOutsideLeipzigEvening)) {
              _tmp_8 = null;
            } else {
              _tmp_8 = _cursor.getInt(_cursorIndexOfOutsideLeipzigEvening);
            }
            _tmpOutsideLeipzigEvening = _tmp_8 == null ? null : _tmp_8 != 0;
            final LocationStatus _tmpEveningLocationStatus;
            _tmpEveningLocationStatus = __LocationStatus_stringToEnum(_cursor.getString(_cursorIndexOfEveningLocationStatus));
            final Long _tmpTravelStartAt;
            if (_cursor.isNull(_cursorIndexOfTravelStartAt)) {
              _tmpTravelStartAt = null;
            } else {
              _tmpTravelStartAt = _cursor.getLong(_cursorIndexOfTravelStartAt);
            }
            final Long _tmpTravelArriveAt;
            if (_cursor.isNull(_cursorIndexOfTravelArriveAt)) {
              _tmpTravelArriveAt = null;
            } else {
              _tmpTravelArriveAt = _cursor.getLong(_cursorIndexOfTravelArriveAt);
            }
            final String _tmpTravelLabelStart;
            if (_cursor.isNull(_cursorIndexOfTravelLabelStart)) {
              _tmpTravelLabelStart = null;
            } else {
              _tmpTravelLabelStart = _cursor.getString(_cursorIndexOfTravelLabelStart);
            }
            final String _tmpTravelLabelEnd;
            if (_cursor.isNull(_cursorIndexOfTravelLabelEnd)) {
              _tmpTravelLabelEnd = null;
            } else {
              _tmpTravelLabelEnd = _cursor.getString(_cursorIndexOfTravelLabelEnd);
            }
            final boolean _tmpNeedsReview;
            final int _tmp_9;
            _tmp_9 = _cursor.getInt(_cursorIndexOfNeedsReview);
            _tmpNeedsReview = _tmp_9 != 0;
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new WorkEntry(_tmpDate,_tmpWorkStart,_tmpWorkEnd,_tmpBreakMinutes,_tmpDayType,_tmpMorningCapturedAt,_tmpMorningLocationLabel,_tmpMorningLat,_tmpMorningLon,_tmpMorningAccuracyMeters,_tmpOutsideLeipzigMorning,_tmpMorningLocationStatus,_tmpEveningCapturedAt,_tmpEveningLocationLabel,_tmpEveningLat,_tmpEveningLon,_tmpEveningAccuracyMeters,_tmpOutsideLeipzigEvening,_tmpEveningLocationStatus,_tmpTravelStartAt,_tmpTravelArriveAt,_tmpTravelLabelStart,_tmpTravelLabelEnd,_tmpNeedsReview,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getByDateRange(final LocalDate startDate, final LocalDate endDate,
      final Continuation<? super List<WorkEntry>> $completion) {
    final String _sql = "SELECT * FROM work_entries WHERE date >= ? AND date <= ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final String _tmp = __localDateConverter.fromLocalDate(startDate);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    _argIndex = 2;
    final String _tmp_1 = __localDateConverter.fromLocalDate(endDate);
    if (_tmp_1 == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp_1);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WorkEntry>>() {
      @Override
      @NonNull
      public List<WorkEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWorkStart = CursorUtil.getColumnIndexOrThrow(_cursor, "workStart");
          final int _cursorIndexOfWorkEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "workEnd");
          final int _cursorIndexOfBreakMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "breakMinutes");
          final int _cursorIndexOfDayType = CursorUtil.getColumnIndexOrThrow(_cursor, "dayType");
          final int _cursorIndexOfMorningCapturedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "morningCapturedAt");
          final int _cursorIndexOfMorningLocationLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLocationLabel");
          final int _cursorIndexOfMorningLat = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLat");
          final int _cursorIndexOfMorningLon = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLon");
          final int _cursorIndexOfMorningAccuracyMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "morningAccuracyMeters");
          final int _cursorIndexOfOutsideLeipzigMorning = CursorUtil.getColumnIndexOrThrow(_cursor, "outsideLeipzigMorning");
          final int _cursorIndexOfMorningLocationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "morningLocationStatus");
          final int _cursorIndexOfEveningCapturedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningCapturedAt");
          final int _cursorIndexOfEveningLocationLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLocationLabel");
          final int _cursorIndexOfEveningLat = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLat");
          final int _cursorIndexOfEveningLon = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLon");
          final int _cursorIndexOfEveningAccuracyMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningAccuracyMeters");
          final int _cursorIndexOfOutsideLeipzigEvening = CursorUtil.getColumnIndexOrThrow(_cursor, "outsideLeipzigEvening");
          final int _cursorIndexOfEveningLocationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningLocationStatus");
          final int _cursorIndexOfTravelStartAt = CursorUtil.getColumnIndexOrThrow(_cursor, "travelStartAt");
          final int _cursorIndexOfTravelArriveAt = CursorUtil.getColumnIndexOrThrow(_cursor, "travelArriveAt");
          final int _cursorIndexOfTravelLabelStart = CursorUtil.getColumnIndexOrThrow(_cursor, "travelLabelStart");
          final int _cursorIndexOfTravelLabelEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "travelLabelEnd");
          final int _cursorIndexOfNeedsReview = CursorUtil.getColumnIndexOrThrow(_cursor, "needsReview");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<WorkEntry> _result = new ArrayList<WorkEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkEntry _item;
            final LocalDate _tmpDate;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_3 = __localDateConverter.toLocalDate(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_3;
            }
            final LocalTime _tmpWorkStart;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfWorkStart)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfWorkStart);
            }
            final LocalTime _tmp_5 = __localTimeConverter.toLocalTime(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalTime', but it was NULL.");
            } else {
              _tmpWorkStart = _tmp_5;
            }
            final LocalTime _tmpWorkEnd;
            final String _tmp_6;
            if (_cursor.isNull(_cursorIndexOfWorkEnd)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getString(_cursorIndexOfWorkEnd);
            }
            final LocalTime _tmp_7 = __localTimeConverter.toLocalTime(_tmp_6);
            if (_tmp_7 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.LocalTime', but it was NULL.");
            } else {
              _tmpWorkEnd = _tmp_7;
            }
            final int _tmpBreakMinutes;
            _tmpBreakMinutes = _cursor.getInt(_cursorIndexOfBreakMinutes);
            final DayType _tmpDayType;
            _tmpDayType = __DayType_stringToEnum(_cursor.getString(_cursorIndexOfDayType));
            final Long _tmpMorningCapturedAt;
            if (_cursor.isNull(_cursorIndexOfMorningCapturedAt)) {
              _tmpMorningCapturedAt = null;
            } else {
              _tmpMorningCapturedAt = _cursor.getLong(_cursorIndexOfMorningCapturedAt);
            }
            final String _tmpMorningLocationLabel;
            if (_cursor.isNull(_cursorIndexOfMorningLocationLabel)) {
              _tmpMorningLocationLabel = null;
            } else {
              _tmpMorningLocationLabel = _cursor.getString(_cursorIndexOfMorningLocationLabel);
            }
            final Double _tmpMorningLat;
            if (_cursor.isNull(_cursorIndexOfMorningLat)) {
              _tmpMorningLat = null;
            } else {
              _tmpMorningLat = _cursor.getDouble(_cursorIndexOfMorningLat);
            }
            final Double _tmpMorningLon;
            if (_cursor.isNull(_cursorIndexOfMorningLon)) {
              _tmpMorningLon = null;
            } else {
              _tmpMorningLon = _cursor.getDouble(_cursorIndexOfMorningLon);
            }
            final Float _tmpMorningAccuracyMeters;
            if (_cursor.isNull(_cursorIndexOfMorningAccuracyMeters)) {
              _tmpMorningAccuracyMeters = null;
            } else {
              _tmpMorningAccuracyMeters = _cursor.getFloat(_cursorIndexOfMorningAccuracyMeters);
            }
            final Boolean _tmpOutsideLeipzigMorning;
            final Integer _tmp_8;
            if (_cursor.isNull(_cursorIndexOfOutsideLeipzigMorning)) {
              _tmp_8 = null;
            } else {
              _tmp_8 = _cursor.getInt(_cursorIndexOfOutsideLeipzigMorning);
            }
            _tmpOutsideLeipzigMorning = _tmp_8 == null ? null : _tmp_8 != 0;
            final LocationStatus _tmpMorningLocationStatus;
            _tmpMorningLocationStatus = __LocationStatus_stringToEnum(_cursor.getString(_cursorIndexOfMorningLocationStatus));
            final Long _tmpEveningCapturedAt;
            if (_cursor.isNull(_cursorIndexOfEveningCapturedAt)) {
              _tmpEveningCapturedAt = null;
            } else {
              _tmpEveningCapturedAt = _cursor.getLong(_cursorIndexOfEveningCapturedAt);
            }
            final String _tmpEveningLocationLabel;
            if (_cursor.isNull(_cursorIndexOfEveningLocationLabel)) {
              _tmpEveningLocationLabel = null;
            } else {
              _tmpEveningLocationLabel = _cursor.getString(_cursorIndexOfEveningLocationLabel);
            }
            final Double _tmpEveningLat;
            if (_cursor.isNull(_cursorIndexOfEveningLat)) {
              _tmpEveningLat = null;
            } else {
              _tmpEveningLat = _cursor.getDouble(_cursorIndexOfEveningLat);
            }
            final Double _tmpEveningLon;
            if (_cursor.isNull(_cursorIndexOfEveningLon)) {
              _tmpEveningLon = null;
            } else {
              _tmpEveningLon = _cursor.getDouble(_cursorIndexOfEveningLon);
            }
            final Float _tmpEveningAccuracyMeters;
            if (_cursor.isNull(_cursorIndexOfEveningAccuracyMeters)) {
              _tmpEveningAccuracyMeters = null;
            } else {
              _tmpEveningAccuracyMeters = _cursor.getFloat(_cursorIndexOfEveningAccuracyMeters);
            }
            final Boolean _tmpOutsideLeipzigEvening;
            final Integer _tmp_9;
            if (_cursor.isNull(_cursorIndexOfOutsideLeipzigEvening)) {
              _tmp_9 = null;
            } else {
              _tmp_9 = _cursor.getInt(_cursorIndexOfOutsideLeipzigEvening);
            }
            _tmpOutsideLeipzigEvening = _tmp_9 == null ? null : _tmp_9 != 0;
            final LocationStatus _tmpEveningLocationStatus;
            _tmpEveningLocationStatus = __LocationStatus_stringToEnum(_cursor.getString(_cursorIndexOfEveningLocationStatus));
            final Long _tmpTravelStartAt;
            if (_cursor.isNull(_cursorIndexOfTravelStartAt)) {
              _tmpTravelStartAt = null;
            } else {
              _tmpTravelStartAt = _cursor.getLong(_cursorIndexOfTravelStartAt);
            }
            final Long _tmpTravelArriveAt;
            if (_cursor.isNull(_cursorIndexOfTravelArriveAt)) {
              _tmpTravelArriveAt = null;
            } else {
              _tmpTravelArriveAt = _cursor.getLong(_cursorIndexOfTravelArriveAt);
            }
            final String _tmpTravelLabelStart;
            if (_cursor.isNull(_cursorIndexOfTravelLabelStart)) {
              _tmpTravelLabelStart = null;
            } else {
              _tmpTravelLabelStart = _cursor.getString(_cursorIndexOfTravelLabelStart);
            }
            final String _tmpTravelLabelEnd;
            if (_cursor.isNull(_cursorIndexOfTravelLabelEnd)) {
              _tmpTravelLabelEnd = null;
            } else {
              _tmpTravelLabelEnd = _cursor.getString(_cursorIndexOfTravelLabelEnd);
            }
            final boolean _tmpNeedsReview;
            final int _tmp_10;
            _tmp_10 = _cursor.getInt(_cursorIndexOfNeedsReview);
            _tmpNeedsReview = _tmp_10 != 0;
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new WorkEntry(_tmpDate,_tmpWorkStart,_tmpWorkEnd,_tmpBreakMinutes,_tmpDayType,_tmpMorningCapturedAt,_tmpMorningLocationLabel,_tmpMorningLat,_tmpMorningLon,_tmpMorningAccuracyMeters,_tmpOutsideLeipzigMorning,_tmpMorningLocationStatus,_tmpEveningCapturedAt,_tmpEveningLocationLabel,_tmpEveningLat,_tmpEveningLon,_tmpEveningAccuracyMeters,_tmpOutsideLeipzigEvening,_tmpEveningLocationStatus,_tmpTravelStartAt,_tmpTravelArriveAt,_tmpTravelLabelStart,_tmpTravelLabelEnd,_tmpNeedsReview,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __DayType_enumToString(@NonNull final DayType _value) {
    switch (_value) {
      case WORK: return "WORK";
      case OFF: return "OFF";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private String __LocationStatus_enumToString(@NonNull final LocationStatus _value) {
    switch (_value) {
      case OK: return "OK";
      case UNAVAILABLE: return "UNAVAILABLE";
      case LOW_ACCURACY: return "LOW_ACCURACY";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private DayType __DayType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "WORK": return DayType.WORK;
      case "OFF": return DayType.OFF;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }

  private LocationStatus __LocationStatus_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "OK": return LocationStatus.OK;
      case "UNAVAILABLE": return LocationStatus.UNAVAILABLE;
      case "LOW_ACCURACY": return LocationStatus.LOW_ACCURACY;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
