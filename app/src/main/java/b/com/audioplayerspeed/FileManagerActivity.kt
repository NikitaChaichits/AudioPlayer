package b.com.audioplayerspeed

import android.app.ListActivity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ListView
import android.widget.SimpleAdapter

import java.util.ArrayList
import java.util.HashMap

class FileManagerActivity : ListActivity() {
    private val ATTRIBUTE_NAME_TEXT = "text"
    private val ATTRIBUTE_NAME_TEXT_AUTHOR = "text_author"

    lateinit var listOfSongs: ArrayList<String>
    lateinit var listOfSongsPath: ArrayList<String>
    lateinit var listOfSongsAuthor: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

        val btnBack : ImageButton = findViewById(R.id.btnBackFolder)

        // достаём из телефона музыку и добавляем в общий список
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cursor = contentResolver.query(
            uri, null,
            MediaStore.Audio.Media.IS_MUSIC + " != 0", null, null
        )
        listOfSongs = ArrayList()
        listOfSongsPath = ArrayList()
        listOfSongsAuthor = ArrayList()
        cursor!!.moveToFirst()

        while (cursor.moveToNext()) {
            val title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
            val data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
            val artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
            listOfSongs.add(title)
            listOfSongsPath.add(data)
            listOfSongsAuthor.add(artist)
        }
        cursor.close()

        val data = ArrayList<Map<String, Any>>(listOfSongs.size)
        var m: MutableMap<String, Any>

        for (i in listOfSongs.indices) {
            m = HashMap()
            if (i == intent.getIntExtra("songPosition", 0)) {
                m[ATTRIBUTE_NAME_TEXT] = listOfSongs[i]
                m[ATTRIBUTE_NAME_TEXT_AUTHOR] = listOfSongsAuthor[i]
            } else {
                m[ATTRIBUTE_NAME_TEXT] = listOfSongs[i]
                m[ATTRIBUTE_NAME_TEXT_AUTHOR] = listOfSongsAuthor[i]
            }
            data.add(m)
        }

        // массив имен атрибутов, из которых будут читаться данные
        val from = arrayOf(ATTRIBUTE_NAME_TEXT, ATTRIBUTE_NAME_TEXT_AUTHOR)
        // массив ID View-компонентов, в которые будут вставлять данные
        val to = intArrayOf(R.id.songTitle, R.id.songAuthor)

        // создаем адаптер
        val sAdapter = SimpleAdapter(
            this@FileManagerActivity, data,
            R.layout.songs_playlist_item, from, to
        )
        listAdapter = sAdapter
        setSelection(intent.getIntExtra("songPosition", 0))

        /**
         * Кнопка назад на главный экран
         */
        btnBack.setOnClickListener { finish() }
    }

    /**
     * Обработчик нажатия на элементы ListView
     */
    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val intent = Intent()
        intent.putExtra("index", position)
        intent.putExtra("songPosition", position)
        intent.putExtra("author", listOfSongsAuthor[position])
        intent.putStringArrayListExtra("listAuthors", listOfSongsAuthor)
        intent.putStringArrayListExtra("listSongsPath", listOfSongsPath)
        intent.putStringArrayListExtra("listSongs", listOfSongs)
        setResult(200, intent)
        finish()
    }

    override fun onBackPressed() {
        finish()
    }
}