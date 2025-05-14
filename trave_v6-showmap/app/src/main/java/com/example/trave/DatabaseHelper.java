package com.example.trave;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.trave.Domains.Itinerary;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.Domains.Sites;
import com.example.trave.Domains.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "travelsv.db";
    private static final int DATABASE_VERSION = 2;

    // Table names
    private static final String TABLE_ITINERARIES = "itineraries";
    private static final String TABLE_SITES = "sites";

    // Itineraries table columns
    private static final String COLUMN_ITINERARY_ID = "id";
    private static final String COLUMN_ITINERARY_TITLE = "title";
    private static final String COLUMN_ITINERARY_LOCATION = "location";
    private static final String COLUMN_ITINERARY_DAYS = "days";
    private static final String COLUMN_ITINERARY_USER_ID = "user_id";
    private static final String COLUMN_ITINERARY_STATUS = "status";

    private static final String COLUMN_SITE_ID = "id";
    private static final String COLUMN_SITE_POI_ID = "poi_id";
    private static final String COLUMN_SITE_NAME = "name";
    private static final String COLUMN_SITE_LATITUDE = "latitude";
    private static final String COLUMN_SITE_LONGITUDE = "longitude";
    private static final String COLUMN_SITE_ADDRESS = "address";
    private static final String COLUMN_SITE_BUSINESS_AREA = "business_area";
    private static final String COLUMN_SITE_TEL = "tel";
    private static final String COLUMN_SITE_WEBSITE = "website";
    private static final String COLUMN_SITE_TYPE_DESC = "type_desc";
    private static final String COLUMN_SITE_PHOTOS = "photos";


    private static final String TABLE_ATTRACTIONS = "Itineraryattractions";
    private static final String COLUMN_ATTRACTION_ID = "id";
    private static final String COLUMN_ITINERARY_SITE_ID = "site_id";
    private static final String COLUMN_ATTRACTION_ITINERARY_ID = "itinerary_id"; // 外键，关联itineraries表的id
    private static final String COLUMN_ATTRACTION_DAY_NUMBER = "day_number";
    private static final String COLUMN_ATTRACTION_VISIT_ORDER = "visit_order";
    private static final String COLUMN_ATTRACTION_NAME = "name";
    private static final String COLUMN_ATTRACTION_TRANSPORT = "transport";
    private static final String COLUMN_ATTRACTION_TYPE = "type";
    private static final String COLUMN_ATTRACTION_IS_AI_RECOMMENDED = "is_ai_recommended";
    private static final String COLUMN_ATTRACTION_AI_RECOMMEND_REASON = "ai_recommend_reason";

    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_USER_NAME = "username";
    private static final String COLUMN_USER_PASSWORD = "password";

    // 创建表的SQL语句
    private static final String SQL_CREATE_TABLE_ITINERARIES = "CREATE TABLE " + TABLE_ITINERARIES + " (" +
            COLUMN_ITINERARY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_ITINERARY_TITLE + " TEXT," +
            COLUMN_ITINERARY_LOCATION + " TEXT," +
            COLUMN_ITINERARY_DAYS + " INTEGER," +
            COLUMN_ITINERARY_USER_ID + " INTEGER," +  // 新增：用户ID
            COLUMN_ITINERARY_STATUS + " INTEGER DEFAULT 0" +  // 新增：默认草稿
            ")";
    private static final String SQL_CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + " (" +
            COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_USER_NAME + " TEXT," +
            COLUMN_USER_PASSWORD + " TEXT" +
            ")";
    private static final String SQL_CREATE_TABLE_SITES = "CREATE TABLE " + TABLE_SITES + " (" +
            COLUMN_SITE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_SITE_POI_ID + " TEXT UNIQUE," +
            COLUMN_SITE_NAME + " TEXT," +
            COLUMN_SITE_LATITUDE + " REAL," +
            COLUMN_SITE_LONGITUDE + " REAL," +
            COLUMN_SITE_ADDRESS + " TEXT," +
            COLUMN_SITE_BUSINESS_AREA + " TEXT," +
            COLUMN_SITE_TEL + " TEXT," +
            COLUMN_SITE_WEBSITE + " TEXT," +
            COLUMN_SITE_TYPE_DESC + " TEXT," +
            COLUMN_SITE_PHOTOS + " TEXT" +
            ")";
    private static final String SQL_CREATE_TABLE_ATTRACTIONS = "CREATE TABLE " + TABLE_ATTRACTIONS + " (" +
            COLUMN_ATTRACTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_ATTRACTION_ITINERARY_ID + " INTEGER," +  // 外键，引用itineraries表的id
            COLUMN_ITINERARY_SITE_ID + " INTEGER," +
            COLUMN_ATTRACTION_DAY_NUMBER + " INTEGER," +
            COLUMN_ATTRACTION_VISIT_ORDER + " INTEGER," +
            COLUMN_ATTRACTION_NAME + " TEXT," +
            COLUMN_ATTRACTION_TRANSPORT + " TEXT," +
            COLUMN_ATTRACTION_TYPE + " TEXT," +  // 新增type字段
            COLUMN_ATTRACTION_IS_AI_RECOMMENDED + " INTEGER DEFAULT 0," +
            COLUMN_ATTRACTION_AI_RECOMMEND_REASON + " TEXT," +
            "FOREIGN KEY (" + COLUMN_ATTRACTION_ITINERARY_ID + ") REFERENCES " +
            TABLE_ITINERARIES + "(" + COLUMN_ITINERARY_ID + ")," +
            "FOREIGN KEY (" + COLUMN_ITINERARY_SITE_ID + ") REFERENCES " +
            TABLE_SITES + "(" + COLUMN_SITE_ID + ")" +
            ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_ITINERARIES);
        db.execSQL(SQL_CREATE_TABLE_USERS);
        db.execSQL(SQL_CREATE_TABLE_ATTRACTIONS);
        db.execSQL(SQL_CREATE_TABLE_SITES);
        Log.d(TAG, "Database tables created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // 添加新列
            db.execSQL("ALTER TABLE " + TABLE_ATTRACTIONS +
                    " ADD COLUMN " + COLUMN_ATTRACTION_IS_AI_RECOMMENDED + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_ATTRACTIONS +
                    " ADD COLUMN " + COLUMN_ATTRACTION_AI_RECOMMEND_REASON + " TEXT");
        }
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITINERARIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTRACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SITES);// 删除 users 表
        onCreate(db);
    }

    // 添加用户
    public long addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, username);
        values.put(COLUMN_USER_PASSWORD, password);
        long userId = db.insert(TABLE_USERS, null, values);
        db.close();
        return userId;
    }

    // 插入新的 site 或获取已存在的 site ID
    public long addOrGetSite(String poiId, String name, double latitude, double longitude, String address,
                             String businessArea, String tel,
                             String website, String typeDesc, String photos) {
        SQLiteDatabase db = this.getWritableDatabase();
        long siteId;

        // 检查是否已存在相同的POI ID
        String[] columns = {COLUMN_SITE_ID};
        String selection = COLUMN_SITE_POI_ID + "=?";
        String[] selectionArgs = {poiId};
        Cursor cursor = db.query(TABLE_SITES, columns, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // 如果已存在，返回现有的ID
            siteId = cursor.getLong(cursor.getColumnIndex(COLUMN_SITE_ID));
            cursor.close();
        } else {
            // 如果不存在，插入新记录
            ContentValues values = new ContentValues();
            values.put(COLUMN_SITE_POI_ID, poiId);
            values.put(COLUMN_SITE_NAME, name);
            values.put(COLUMN_SITE_LATITUDE, latitude);
            values.put(COLUMN_SITE_LONGITUDE, longitude);
            values.put(COLUMN_SITE_ADDRESS, address);
            values.put(COLUMN_SITE_BUSINESS_AREA, businessArea);
            values.put(COLUMN_SITE_TEL, tel);
            values.put(COLUMN_SITE_WEBSITE, website);
            values.put(COLUMN_SITE_TYPE_DESC, typeDesc);
            values.put(COLUMN_SITE_PHOTOS, photos);

            siteId = db.insert(TABLE_SITES, null, values);
        }

        db.close();
        return siteId;
    }

    // 验证用户登录
    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_USER_NAME + "=? AND " + COLUMN_USER_PASSWORD + "=?";
        String[] selectionArgs = {username, password};
        Cursor cursor = db.query(TABLE_USERS, null, selection, selectionArgs, null, null, null);
        boolean isValid = (cursor != null && cursor.moveToFirst());
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return isValid;
    }

    // 添加行程单到数据库
    public long addItinerary(Itinerary itinerary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITINERARY_TITLE, itinerary.getTittle());
        values.put(COLUMN_ITINERARY_LOCATION, itinerary.getLocation());
        values.put(COLUMN_ITINERARY_DAYS, itinerary.getDays());
        values.put(COLUMN_ITINERARY_USER_ID, itinerary.getUserId()); // 添加用户ID
        values.put(COLUMN_ITINERARY_STATUS, itinerary.getStatus());  // 添加发布状态
        long id = db.insert(TABLE_ITINERARIES, null, values);
        itinerary.setId(id);
        db.close();
        return id;
    }

    // 删除特定行程的所有景点数据
    public void deleteAttractionsForItinerary(long itineraryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_ATTRACTION_ITINERARY_ID + "=?";
        String[] whereArgs = {String.valueOf(itineraryId)};
        db.delete(TABLE_ATTRACTIONS, whereClause, whereArgs);
        db.close();
    }

    // 通过用户名获取用户ID
    public long getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_USER_NAME + "=?";
        String[] selectionArgs = {username};
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID}, selection, selectionArgs, null, null, null);

        long userId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getLong(cursor.getColumnIndex(COLUMN_USER_ID));
            cursor.close();
        }
        db.close();
        return userId;
    }

    // 通过行程单id获得行程单
    public Itinerary getItineraryById(long itineraryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_ITINERARY_ID + "=?";
        String[] selectionArgs = {String.valueOf(itineraryId)};
        Cursor cursor = db.query(TABLE_ITINERARIES, null, selection, selectionArgs, null, null, null);
        Itinerary itinerary = null;
        if (cursor != null && cursor.moveToFirst()) {
            // 获取所有列名
            String[] columnNames = cursor.getColumnNames();
            Log.d("DatabaseHelper", "列名: " + Arrays.toString(columnNames));

            // 打印所有列的值
            for (String columnName : columnNames) {
                int columnIndex = cursor.getColumnIndex(columnName);
                String value = cursor.getString(columnIndex);
                Log.d("DatabaseHelper", columnName + ": " + value);
            }

            String title = cursor.getString(cursor.getColumnIndex(COLUMN_ITINERARY_TITLE));
            String location = cursor.getString(cursor.getColumnIndex(COLUMN_ITINERARY_LOCATION));
            int days = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_DAYS));
            int pic = ThreadLocalRandom.current().nextInt(1, 4);
            int status = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_STATUS));
            long userId = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_USER_ID));

            Log.d("DatabaseHelper", "获取行程详情 - ID: " + itineraryId +
                    ", 标题: " + title +
                    ", 位置: " + location +
                    ", 天数: " + days +
                    ", 状态: " + status +
                    ", 用户ID: " + userId);

            itinerary = new Itinerary(itineraryId, title, location, "pic" + pic, days, userId, status);
            cursor.close();
        } else {
            Log.e("DatabaseHelper", "未找到ID为 " + itineraryId + " 的行程");
        }

        return itinerary;
    }

    // 删除特定行程单及其所有相关景点
    public boolean deleteItinerary(long itineraryId) {
        SQLiteDatabase db = this.getWritableDatabase();

        String whereClause1 = COLUMN_ATTRACTION_ITINERARY_ID + "=?";
        String[] whereArgs1 = {String.valueOf(itineraryId)};
        db.delete(TABLE_ATTRACTIONS, whereClause1, whereArgs1);

        String whereClause = COLUMN_ITINERARY_ID + "=?";
        String[] whereArgs = {String.valueOf(itineraryId)};
        int rowsAffected = db.delete(TABLE_ITINERARIES, whereClause, whereArgs);

        db.close();
        return rowsAffected > 0; // 返回是否成功删除行程单
    }

    // 通过用户id获得行程单
    public ArrayList<Itinerary> getUserItineraries(long userId) {
        ArrayList<Itinerary> itineraries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_ITINERARY_USER_ID + "=?";
        String[] selectionArgs = {String.valueOf(userId)};

        Log.d("DatabaseHelper", "Querying itineraries for userId: " + userId);

        Cursor cursor = db.query(TABLE_ITINERARIES, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_ID));
                String title = cursor.getString(cursor.getColumnIndex(COLUMN_ITINERARY_TITLE));
                String location = cursor.getString(cursor.getColumnIndex(COLUMN_ITINERARY_LOCATION));
                int days = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_DAYS));
                int status = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_STATUS));

                Itinerary itinerary = new Itinerary(id, title, location, "pic1", days);
                itineraries.add(itinerary);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return itineraries;
    }


    // 添加景点详情到数据库
    public long addAttraction(ItineraryAttraction attraction) {
        if (attraction == null) {
            Log.e(TAG, "addAttraction: attraction is null");
            return -1;
        }

        if (attraction.getItineraryId() <= 0) {
            Log.e(TAG, "addAttraction: invalid itineraryId: " + attraction.getItineraryId());
            return -1;
        }

        if (attraction.getSiteId() <= 0) {
            Log.e(TAG, "addAttraction: invalid siteId: " + attraction.getSiteId());
            return -1;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;

        try {
            db.beginTransaction();

            // 获取site的type_desc并判断类型
            Sites site = getSiteBySiteId(attraction.getSiteId(), db);
            if (site != null) {
                String type = determineAttractionType(site.getTypeDesc());
                attraction.setType(type);
                Log.d(TAG, "设置景点类型: " + type + " (typeDesc: " + site.getTypeDesc() + ")");
            } else {
                Log.w(TAG, "无法找到景点信息，使用默认类型: 景点");
                attraction.setType("景点");
            }

            ContentValues values = new ContentValues();
            values.put(COLUMN_ITINERARY_SITE_ID, attraction.getSiteId());
            values.put(COLUMN_ATTRACTION_ITINERARY_ID, attraction.getItineraryId());
            values.put(COLUMN_ATTRACTION_DAY_NUMBER, attraction.getDayNumber());
            values.put(COLUMN_ATTRACTION_VISIT_ORDER, attraction.getVisitOrder());
            values.put(COLUMN_ATTRACTION_NAME, attraction.getAttractionName());
            values.put(COLUMN_ATTRACTION_TRANSPORT, attraction.getTransport());
            values.put(COLUMN_ATTRACTION_TYPE, attraction.getType());

            // 添加日志记录所有值
            Log.d(TAG, "添加景点 - " +
                    "siteId: " + attraction.getSiteId() + ", " +
                    "itineraryId: " + attraction.getItineraryId() + ", " +
                    "dayNumber: " + attraction.getDayNumber() + ", " +
                    "visitOrder: " + attraction.getVisitOrder() + ", " +
                    "name: " + attraction.getAttractionName() + ", " +
                    "transport: " + attraction.getTransport() + ", " +
                    "type: " + attraction.getType());

            id = db.insert(TABLE_ATTRACTIONS, null, values);

            if (id == -1) {
                Log.e(TAG, "插入景点失败");
            } else {
                Log.d(TAG, "成功插入景点，ID: " + id);
                attraction.setId(id);
                db.setTransactionSuccessful();
            }

        } catch (Exception e) {
            Log.e(TAG, "添加景点时出错: " + e.getMessage());
            id = -1;
        } finally {
            try {
                db.endTransaction();
                db.close();
            } catch (Exception e) {
                Log.e(TAG, "关闭数据库时出错: " + e.getMessage());
            }
        }

        return id;
    }

    // 获取所有行程单
    public ArrayList<Itinerary> getAllItineraries() {
        ArrayList<Itinerary> itineraries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ITINERARIES, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_ID));
                String title = cursor.getString(cursor.getColumnIndex(COLUMN_ITINERARY_TITLE));
                String location = cursor.getString(cursor.getColumnIndex(COLUMN_ITINERARY_LOCATION));
                int days = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_DAYS));
                int userId = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_USER_ID));  // 获取user_id
                int status = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_STATUS));  // 获取发布状态
                Itinerary itinerary = new Itinerary(id, title, location, "pic3", days, userId, status); // 更新构造函数
                itineraries.add(itinerary);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return itineraries;
    }

    // 获取用户数量
    public int getUserSize() {
        int size = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                size++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return size;
    }

    // 获取所有已发布的行程单
    public ArrayList<Itinerary> getAllPublishItineraries() {
        ArrayList<Itinerary> itineraries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 添加条件筛选，只查询status为true的行程单
        String selection = COLUMN_ITINERARY_STATUS + "=?";
        String[] selectionArgs = {"1"};  // "1" 表示 true

        Cursor cursor = db.query(TABLE_ITINERARIES, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_ID));
                String title = cursor.getString(cursor.getColumnIndex(COLUMN_ITINERARY_TITLE));
                String location = cursor.getString(cursor.getColumnIndex(COLUMN_ITINERARY_LOCATION));
                int days = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_DAYS));
                int userId = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_USER_ID));  // 获取user_id
                int status = cursor.getInt(cursor.getColumnIndex(COLUMN_ITINERARY_STATUS));  // 获取发布状态

                // 仅添加已发布（status = true）的行程单
                if (status == 1) {
                    Itinerary itinerary = new Itinerary(id, title, location, "pic3", days, userId, status); // 更新构造函数
                    itineraries.add(itinerary);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return itineraries;
    }

    public long copyItineraryToCurrentUser(long originalItineraryId, long userId) {
        SQLiteDatabase db = this.getWritableDatabase(); // 打开数据库连接

        try {
            // 获取原始行程单数据
            Itinerary originalItinerary = getItineraryById(originalItineraryId);
            if (originalItinerary == null) {
                return -1; // 原始行程单不存在
            }

            // 创建新的行程单数据，并将其所有者变更为当前登录用户
            ContentValues values = new ContentValues();
            values.put(COLUMN_ITINERARY_TITLE, originalItinerary.getTittle());
            values.put(COLUMN_ITINERARY_LOCATION, originalItinerary.getLocation());
            values.put(COLUMN_ITINERARY_DAYS, originalItinerary.getDays());
            values.put(COLUMN_ITINERARY_USER_ID, userId); // 更新为当前登录用户的 ID
            values.put(COLUMN_ITINERARY_STATUS, 0);  // 新增的行程单默认为草稿状态（未发布）

            long newItineraryId = db.insert(TABLE_ITINERARIES, null, values); // 插入新行程单

            if (newItineraryId > 0) {
                // 复制原行程单中的所有景点到新的行程单
                ArrayList<ItineraryAttraction> originalAttractions = getItineraryAttractions(originalItineraryId);
                for (ItineraryAttraction attraction : originalAttractions) {
                    attraction.setItineraryId(newItineraryId); // 更新为新的行程单ID
                    addAttraction(attraction);  // 插入新行程单的景点
                }
            }

            return newItineraryId; // 返回新行程单的 ID
        } finally {
            // 确保在方法结束时关闭数据库连接
            db.close();
        }
    }


    // 更新行程单状态为发布（status = 1）
    public boolean publishItinerary(long itineraryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITINERARY_STATUS, 1);  // 1表示已发布

        String whereClause = COLUMN_ITINERARY_ID + "=?";
        String[] whereArgs = {String.valueOf(itineraryId)};

        int rowsAffected = db.update(TABLE_ITINERARIES, values, whereClause, whereArgs);
        db.close();
        return rowsAffected > 0; // 返回是否成功更新数据
    }

    // 获取指定行程的所有景点
    public ArrayList<ItineraryAttraction> getItineraryAttractions(long itineraryId) {
        ArrayList<ItineraryAttraction> attractions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            String selection = COLUMN_ATTRACTION_ITINERARY_ID + "=?";
            String[] selectionArgs = {String.valueOf(itineraryId)};
            Cursor cursor = db.query(TABLE_ATTRACTIONS, null, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // 获取列索引
                    int siteIdIndex = cursor.getColumnIndex(COLUMN_ITINERARY_SITE_ID);
                    int dayNumberIndex = cursor.getColumnIndex(COLUMN_ATTRACTION_DAY_NUMBER);
                    int visitOrderIndex = cursor.getColumnIndex(COLUMN_ATTRACTION_VISIT_ORDER);
                    int nameIndex = cursor.getColumnIndex(COLUMN_ATTRACTION_NAME);
                    int transportIndex = cursor.getColumnIndex(COLUMN_ATTRACTION_TRANSPORT);
                    int typeIndex = cursor.getColumnIndex(COLUMN_ATTRACTION_TYPE);

                    // AI推荐相关的列索引（可能不存在）
                    int isAiRecommendedIndex = cursor.getColumnIndex(COLUMN_ATTRACTION_IS_AI_RECOMMENDED);
                    int aiRecommendReasonIndex = cursor.getColumnIndex(COLUMN_ATTRACTION_AI_RECOMMEND_REASON);

                    // 检查列是否存在
                    if (siteIdIndex == -1 || dayNumberIndex == -1 || visitOrderIndex == -1 ||
                            nameIndex == -1 || transportIndex == -1) {
                        Log.e(TAG, "某些必需的列不存在");
                        continue;
                    }

                    // 读取数据
                    long siteId = cursor.getLong(siteIdIndex);
                    int dayNumber = cursor.getInt(dayNumberIndex);
                    int visitOrder = cursor.getInt(visitOrderIndex);
                    String name = cursor.getString(nameIndex);
                    String transport = cursor.getString(transportIndex);

                    // 创建ItineraryAttraction对象
                    ItineraryAttraction attraction = new ItineraryAttraction(itineraryId, siteId, dayNumber, visitOrder, name, transport);

                    // 设置类型（如果列存在）
                    if (typeIndex != -1) {
                        String type = cursor.getString(typeIndex);
                        attraction.setType(type != null ? type : "景点");
                    } else {
                        attraction.setType("景点");
                    }

                    // 设置AI推荐相关字段（如果列存在）
                    try {
                        if (isAiRecommendedIndex != -1) {
                            attraction.setAiRecommended(cursor.getInt(isAiRecommendedIndex) == 1);
                        }

                        if (aiRecommendReasonIndex != -1) {
                            attraction.setAiRecommendReason(cursor.getString(aiRecommendReasonIndex));
                        }
                    } catch (Exception e) {
                        // 如果AI推荐相关列读取失败，设置默认值
                        Log.w(TAG, "读取AI推荐信息失败，使用默认值: " + e.getMessage());
                        attraction.setAiRecommended(false);
                        attraction.setAiRecommendReason("");
                    }

                    attractions.add(attraction);
                } while (cursor.moveToNext());
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取行程景点时出错: " + e.getMessage());
        } finally {
            db.close();
        }

        return attractions;
    }

    // 更新行程的所有字段
    public boolean updateItinerary(Itinerary itinerary) {
        if (itinerary.getId() <= 0) {
            return false; // 无效的行程 ID
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITINERARY_TITLE, itinerary.getTittle());
        values.put(COLUMN_ITINERARY_LOCATION, itinerary.getLocation());
        values.put(COLUMN_ITINERARY_DAYS, itinerary.getDays());
        String whereClause = COLUMN_ITINERARY_ID + "=?";
        String[] whereArgs = {String.valueOf(itinerary.getId())};

        int rowsAffected = db.update(TABLE_ITINERARIES, values, whereClause, whereArgs);
        db.close();
        return rowsAffected > 0; // 返回是否成功更新数据
    }

    public Sites getSiteBySiteId(long siteId, SQLiteDatabase db) {
        String selection = COLUMN_SITE_ID + "=?";
        String[] selectionArgs = {String.valueOf(siteId)};
        Cursor cursor = db.query(TABLE_SITES, null, selection, selectionArgs, null, null, null);
        Sites site = null;
        if (cursor != null && cursor.moveToFirst()) {
            site = new Sites();
            site.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_SITE_ID)));
            site.setPoiId(cursor.getString(cursor.getColumnIndex(COLUMN_SITE_POI_ID)));
            site.setName(cursor.getString(cursor.getColumnIndex(COLUMN_SITE_NAME)));
            site.setLatitude(cursor.getDouble(cursor.getColumnIndex(COLUMN_SITE_LATITUDE)));
            site.setLongitude(cursor.getDouble(cursor.getColumnIndex(COLUMN_SITE_LONGITUDE)));
            site.setAddress(cursor.getString(cursor.getColumnIndex(COLUMN_SITE_ADDRESS)));
            site.setBusinessArea(cursor.getString(cursor.getColumnIndex(COLUMN_SITE_BUSINESS_AREA)));
            site.setTel(cursor.getString(cursor.getColumnIndex(COLUMN_SITE_TEL)));
            site.setWebsite(cursor.getString(cursor.getColumnIndex(COLUMN_SITE_WEBSITE)));
            site.setTypeDesc(cursor.getString(cursor.getColumnIndex(COLUMN_SITE_TYPE_DESC)));
            site.setPhotos(cursor.getString(cursor.getColumnIndex(COLUMN_SITE_PHOTOS)));
            cursor.close();
        }
        return site;
    }

    // 为了向后兼容，保留原有方法
    public Sites getSiteBySiteId(long siteId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Sites site = getSiteBySiteId(siteId, db);
        db.close();
        return site;
    }

    // 根据type_desc判断类型
    public String determineAttractionType(String typeDesc) {
        if (typeDesc != null) {
            if (typeDesc.contains("餐饮服务")) {
                return "餐厅";
            } else if (typeDesc.contains("住宿服务")) {
                return "酒店";
            }
        }
        return "景点";
    }

    public boolean deleteAttractionByDayAndOrder(long itineraryId, int dayNumber, int visitOrder) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_ATTRACTION_ITINERARY_ID + "=? AND " +
                COLUMN_ATTRACTION_DAY_NUMBER + "=? AND " +
                COLUMN_ATTRACTION_VISIT_ORDER + "=?";
        String[] whereArgs = {
                String.valueOf(itineraryId),
                String.valueOf(dayNumber),
                String.valueOf(visitOrder)
        };

        int rowsAffected = db.delete(TABLE_ATTRACTIONS, whereClause, whereArgs);
        db.close();
        return rowsAffected > 0;
    }

    public boolean updateAttractionAiRecommended(long attractionId, boolean isRecommended, String reason) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put(COLUMN_ATTRACTION_IS_AI_RECOMMENDED, isRecommended ? 1 : 0);
            values.put(COLUMN_ATTRACTION_AI_RECOMMEND_REASON, reason);

            // 尝试更新数据
            int result = db.update(TABLE_ATTRACTIONS, values,
                    COLUMN_ATTRACTION_ID + " = ?",
                    new String[]{String.valueOf(attractionId)});

            db.close();
            return result > 0;
        } catch (Exception e) {
            // 如果列不存在，记录日志但不影响程序正常运行
            Log.w(TAG, "更新AI推荐信息失败（可能是列不存在）: " + e.getMessage());
            return false;
        }
    }

    // 更新景点的访问顺序
    public boolean updateAttractionOrder(long attractionId, int dayNumber, int visitOrder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_ATTRACTION_DAY_NUMBER, dayNumber);
        values.put(COLUMN_ATTRACTION_VISIT_ORDER, visitOrder);

        return db.update(TABLE_ATTRACTIONS, values,
                COLUMN_ATTRACTION_ID + " = ?",
                new String[]{String.valueOf(attractionId)}) > 0;
    }
}

