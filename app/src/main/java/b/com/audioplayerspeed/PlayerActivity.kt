package b.com.audioplayerspeed

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

class PlayerActivity : AppCompatActivity(), OnCompletionListener, SeekBar.OnSeekBarChangeListener {

    private lateinit var btnPlay : ImageButton
    private lateinit var songSeekBar: SeekBar
    private lateinit var tvSongTitle: TextView
    private lateinit var tvSongAuthor: TextView
    private lateinit var tvSongCurrentDuration: TextView
    private lateinit var tvSongTotalDuration: TextView
    // Media Player
    private lateinit var mp: MediaPlayer
    // обработчик для обновления таймера
    private val mHandler = Handler()
    private lateinit var utils: ConvertTime

    private var isShuffle = false
    private var isRepeat = false

    private lateinit var songsList: List<String>
    private lateinit var songsListPath: List<String>
    private lateinit var authorsList: List<String>

    var currentSongIndex = 0
    var itemPosition = -1
    var speed = 1.0f

    /**
     * Метод для фонового изменения времени
     */
    private val mUpdateTimeTask = object : Runnable {
        override fun run() {
            val totalDuration = mp.duration.toLong()
            val currentDuration = mp.currentPosition.toLong()

            // Показываем текущее время
            tvSongTotalDuration.text = "" + utils.milliSecondsToTimer(totalDuration)
            // Показываем конечное время
            tvSongCurrentDuration.text = "" + utils.milliSecondsToTimer(currentDuration)

            songSeekBar.max = totalDuration.toInt()
            songSeekBar.progress = currentDuration.toInt()

            mHandler.postDelayed(this, 1000)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        btnPlay = findViewById(R.id.btnPlay)
        val btnPlaylist : ImageButton = findViewById(R.id.btnPlaylist)
        val btnNext : ImageButton = findViewById(R.id.btnNext)
        val btnPrev : ImageButton = findViewById(R.id.btnPrevious)
        val btnBackward : ImageButton = findViewById(R.id.btnBackward)
        val btnForward : ImageButton = findViewById(R.id.btnForward)
        val btnRepeat : ImageButton = findViewById(R.id.btnRepeat)
        val btnShuffle : ImageButton = findViewById(R.id.btnShuffle)
        val btnBlock : ImageButton = findViewById(R.id.btnBlock)
        val btnFavorite : ImageButton = findViewById(R.id.btnFavorite)

        songSeekBar = findViewById(R.id.songSeekBar)
        tvSongTitle = findViewById(R.id.tv_songTitle)
        tvSongAuthor = findViewById(R.id.tv_songAuthor)
        tvSongCurrentDuration = findViewById(R.id.tvCurrentTime)
        tvSongTotalDuration = findViewById(R.id.tvSongTime)

        val spinner : Spinner = findViewById(R.id.spinner)
        val adapter = ArrayAdapter.createFromResource(
            this, R.array.speedlist,
            R.layout.spinner_custom
        )
        adapter.setDropDownViewResource(R.layout.spinner_custom)
        spinner.setSelection(0)
        spinner.adapter = adapter

        // Mediaplayer
        mp = MediaPlayer()
        utils = ConvertTime()

        // Listeners
        mp.setOnCompletionListener(this)
        songSeekBar.setOnSeekBarChangeListener(this)

        btnPlay.setOnClickListener(View.OnClickListener {
            //проверяем разрешение на чтение карты
            val permissionStatus = ContextCompat.checkSelfPermission(
                this@PlayerActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                // проверяем выбрана ли песня
                if (tvSongTitle.text.toString() == resources.getString(R.string.songTitle)) {
                    openPlaylist()
                    return@OnClickListener
                }
                if (mp.isPlaying) { //Остановка песни
                    mp.pause()
                    btnPlay.setImageResource(R.drawable.ic_play_circle)
                } else {
                    // Возобновление песни
                    mp.start()
                    btnPlay.setImageResource(R.drawable.ic_pause_circle)
                }
            } else {
                ActivityCompat.requestPermissions(
                    this@PlayerActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    1
                )
            }
        })
        /**
         * Следующая песня
         */
        btnNext.setOnClickListener(View.OnClickListener { arg0 ->
            // проверяем выбрана ли песня
            if (tvSongTitle.text.toString() == resources.getString(R.string.songTitle)) {
                openPlaylist()
                return@OnClickListener
            }
            // Проверяем есть ли следующая песня
            if (isShuffle) {
                val rand = Random()
                currentSongIndex = rand.nextInt(songsList.size - 1 + 1)
                playSong(currentSongIndex)
                itemPosition = currentSongIndex
            } else {
                if (currentSongIndex < songsList.size - 1) {
                    playSong(currentSongIndex + 1)
                    currentSongIndex = currentSongIndex + 1
                    itemPosition = currentSongIndex
                } else {
                    // играм первую песню
                    playSong(0)
                    currentSongIndex = 0
                    itemPosition = currentSongIndex
                }
            }
        })
        /**
         * Предыдущая песня
         */
        btnPrev.setOnClickListener(View.OnClickListener { arg0 ->
            // проверяем выбрана ли песня
            if (tvSongTitle.text.toString() == resources.getString(R.string.songTitle)) {
                openPlaylist()
                return@OnClickListener
            }
            //Проверяем есть ли предыдущая песня
            if (isShuffle) {
                val rand = Random()
                currentSongIndex = rand.nextInt(songsList.size - 1 + 1)
                playSong(currentSongIndex)
                itemPosition = currentSongIndex
            } else {
                if (currentSongIndex > 0) {
                    playSong(currentSongIndex - 1)
                    currentSongIndex -= 1
                    itemPosition = currentSongIndex
                } else {
                    // играем последюю песню
                    playSong(songsList.size - 1)
                    currentSongIndex = songsList.size - 1
                    itemPosition = currentSongIndex
                }
            }
        })

        /**
         * Повторение песни
         */
        btnRepeat.setOnClickListener {
            if (isRepeat) {
                isRepeat = false
                Toast.makeText(applicationContext, "Повторение выключено", Toast.LENGTH_SHORT)
                    .show()
                btnRepeat.setImageResource(R.drawable.ic_replay)
            } else {

                isRepeat = true
                Toast.makeText(applicationContext, "Повторение включено", Toast.LENGTH_SHORT).show()
                btnRepeat.setImageResource(R.drawable.ic_replay_on)
            }
        }

        /**
         * Случайный порядок
         */
        btnShuffle.setOnClickListener {
            if (isShuffle) {
                isShuffle = false
                Toast.makeText(applicationContext, "Случайный порядок выключен", Toast.LENGTH_SHORT)
                    .show()
                btnShuffle.setImageResource(R.drawable.ic_shuffle)
            } else {
                isShuffle = true
                Toast.makeText(applicationContext, "Случайный порядок включён", Toast.LENGTH_SHORT)
                    .show()
                btnShuffle.setImageResource(R.drawable.ic_shuffle_on)
            }
        }

        /**
         * Вызов плэйлиста
         */
        btnPlaylist.setOnClickListener { arg0 -> openPlaylist() }

        /**
         * Отмотка на 10 сек назад
         */
        btnBackward.setOnClickListener(View.OnClickListener {
            if (tvSongTitle.text.toString() == resources.getString(R.string.songTitle)) {
                return@OnClickListener
            }
            mHandler.removeCallbacks(mUpdateTimeTask)
            // перемотка времени
            mp.seekTo(mp.currentPosition - 10000)
            updateSeekBar()
        })

        /**
         * Перемотка на 10 сек вперед
         */
        btnForward.setOnClickListener(View.OnClickListener {
            if (tvSongTitle.text.toString() == resources.getString(R.string.songTitle)) {
                return@OnClickListener
            }
            mHandler.removeCallbacks(mUpdateTimeTask)

            mp.seekTo(mp.currentPosition + 10000)
            updateSeekBar()
        })

        /**
         * Изменение скорости воспроизведения
         */
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                itemSelected: View, selectedItemPosition: Int, selectedId: Long
            ) {
                val choose = resources.getStringArray(R.array.speedlist)
                val strSpeed = choose[selectedItemPosition]
                speed = (strSpeed.substring(0, strSpeed.length - 1)).toFloat()
                //				playSong(currentSongIndex);
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

    }

    /**
     * Метод для воспроизведения музыки
     */
    fun playSong(songIndex: Int) {
        try {
            mp.reset()

            mp.setDataSource("/" + songsListPath[songIndex])
            mp.playbackParams = mp.playbackParams.setSpeed(speed)

            val songTitle = songsList[songIndex]
            val songAuthor = authorsList[songIndex]

            mp.prepare()
            mp.start()

            // Отображение названия песни после воспроизведения
            tvSongTitle.text = songTitle
            tvSongAuthor.text = songAuthor

            btnPlay.setImageResource(R.drawable.ic_pause_circle)

            // Установка значений SeekBar
            songSeekBar.progress = 0
            songSeekBar.max = 100

            // Обновления SeekBar
            updateSeekBar()

        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    /**
     * Метод для обновления времни
     */
    private fun updateSeekBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100)
    }


    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        if (tvSongTitle.text.toString() == resources.getString(R.string.songTitle)) {
            return
        }
        mHandler.removeCallbacks(mUpdateTimeTask)
    }

    /**
     * Метод для обработки момента, когда отпускается SeekBar
     */
    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (tvSongTitle.text.toString() == resources.getString(R.string.songTitle)) {
            return
        }
        mHandler.removeCallbacks(mUpdateTimeTask)
        // перемотка времени
        mp.seekTo(seekBar.progress)

        //обновление
        updateSeekBar()
    }

    /**
     * Метод для работы повтора и шафла
     */
    override fun onCompletion(arg0: MediaPlayer) {

        // Проверяем состояние повтора
        if (isRepeat) {
            // песня играет по кругу
            playSong(currentSongIndex)
        } else if (isShuffle) {
            // играет в случайном порядке
            val rand = Random()
            currentSongIndex = rand.nextInt(songsList.size)
            playSong(currentSongIndex)
        } else {
            //если не то и не другое играет следующую песню
            if (currentSongIndex < songsList.size - 1) {
                playSong(currentSongIndex + 1)
                currentSongIndex = currentSongIndex + 1
                itemPosition = currentSongIndex
            } else {
                // если нет следующуей играет первую
                playSong(0)
                currentSongIndex = 0
                itemPosition = currentSongIndex
            }
        }
    }

    private fun openPlaylist() {
        val i = Intent(applicationContext, FileManagerActivity::class.java)
        i.putExtra("songPosition", itemPosition)
        startActivityForResult(i, 100)
    }

    /**
     * Получение индекса песни из плэйлиста и её воспроизведение
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_CANCELED) {
            return
        }

        if (resultCode == 100) {
            itemPosition = -1
            songsList = data!!.getStringArrayListExtra("listSongs")
            songsListPath = data.getStringArrayListExtra("listSongsPath")
            authorsList = data.getStringArrayListExtra("listAuthors")
            currentSongIndex = data.extras.getInt("songIndex")
            playSong(currentSongIndex)
        } else {
            songsList = data!!.getStringArrayListExtra("listSongs")
            songsListPath = data.getStringArrayListExtra("listSongsPath")
            authorsList = data.getStringArrayListExtra("listAuthors")
            currentSongIndex = data.extras.getInt("index")
            itemPosition = data.getIntExtra("songPosition", 0)
            playSong(currentSongIndex)
        }

    }

    public override fun onDestroy() {
        super.onDestroy()
        mp.release()
        mHandler.removeCallbacks(mUpdateTimeTask)
    }
}