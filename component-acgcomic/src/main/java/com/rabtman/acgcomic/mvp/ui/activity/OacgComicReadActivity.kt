package com.rabtman.acgcomic.mvp.ui.activity

import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import com.alibaba.android.arouter.facade.annotation.Route
import com.rabtman.acgcomic.R
import com.rabtman.acgcomic.R2
import com.rabtman.acgcomic.base.constant.IntentConstant
import com.rabtman.acgcomic.di.DaggerOacgComicEpisodeDetailComponent
import com.rabtman.acgcomic.di.OacgComicEpisodeDetailModule
import com.rabtman.acgcomic.mvp.OacgComicEpisodeDetailContract
import com.rabtman.acgcomic.mvp.model.entity.OacgComicEpisodePage
import com.rabtman.acgcomic.mvp.presenter.OacgComicEpisodeDetailPresenter
import com.rabtman.acgcomic.mvp.ui.adapter.OacgComicReadAdapter
import com.rabtman.common.base.BaseActivity
import com.rabtman.common.di.component.AppComponent
import com.rabtman.router.RouterConstants


/**
 * @author Rabtman
 */
@Route(path = RouterConstants.PATH_COMIC_OACG_READ)
class OacgComicReadActivity : BaseActivity<OacgComicEpisodeDetailPresenter>(), OacgComicEpisodeDetailContract.View {

    //漫画阅读控制栏
    @BindView(R2.id.layout_comic_top)
    lateinit var layoutComicTop: RelativeLayout
    @BindView(R2.id.btn_comic_back)
    lateinit var btnComicBack: ImageView
    @BindView(R2.id.tv_comic_title)
    lateinit var tvComicTitle: TextView
    @BindView(R2.id.layout_comic_bottom)
    lateinit var layoutComicBottom: RelativeLayout
    @BindView(R2.id.btn_comic_before)
    lateinit var btnComicBefore: ImageView
    @BindView(R2.id.btn_comic_next)
    lateinit var btnComicNext: ImageView
    @BindView(R2.id.seekbar_comic_proc)
    lateinit var seekComicProc: AppCompatSeekBar
    @BindView(R2.id.tv_comic_count)
    lateinit var tvComicCount: TextView
    @BindView(R2.id.tv_comic_pos)
    lateinit var tvComicPos: TextView
    @BindView(R2.id.tv_comic_pos_tip)
    lateinit var tvComicPosTip: TextView
    /**
     * 控制栏的显示状态
     */
    private var mVisible: Boolean = false
    /**
     * 动画时间
     */
    private val UI_ANIMATION_DELAY = 200
    //漫画内容
    @BindView(R2.id.rcv_oacg_comic_content)
    lateinit var rcvOacgComicContent: RecyclerView
    lateinit var layoutManager: LinearLayoutManager
    private lateinit var oacgComicReadAdapter: OacgComicReadAdapter
    //当前漫画名称
    private var curComicTitle = ""
    //当前漫画页面下标
    private var curPagePos = 0
    //当前漫画总页数
    private var maxPage = 0

    override fun setupActivityComponent(appComponent: AppComponent?) {
        DaggerOacgComicEpisodeDetailComponent.builder()
                .appComponent(appComponent)
                .oacgComicEpisodeDetailModule(OacgComicEpisodeDetailModule(this))
                .build()
                .inject(this)
    }

    override fun getLayoutId(): Int {
        return R.layout.acgcomic_activity_oacg_comic_content
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val decorView = window.decorView
            //沉浸
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun setStatusBar() {}

    override fun onPause() {
        super.onPause()
        rcvOacgComicContent.stopScroll()   //防止销毁时再回调滑动事件造成的异常
    }

    override fun initData() {
        curComicTitle = intent.getStringExtra(IntentConstant.OACG_COMIC_TITLE)

        oacgComicReadAdapter = OacgComicReadAdapter(mAppComponent.imageLoader())
        oacgComicReadAdapter.setOnItemClickListener { _, _, _ ->
            toogleDisplayPanel()
        }
        layoutManager = LinearLayoutManager(this)
        rcvOacgComicContent.layoutManager = layoutManager
        rcvOacgComicContent.adapter = oacgComicReadAdapter
        rcvOacgComicContent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val pos = layoutManager.findFirstVisibleItemPosition()
                if (maxPage > 0 && maxPage == layoutManager.findLastCompletelyVisibleItemPosition() + 1) {
                    curPagePos = maxPage - 1
                    seekComicProc.progress = maxPage - 1
                    tvComicPos.text = maxPage.toString()
                } else if (curPagePos != pos) {
                    curPagePos = pos
                    seekComicProc.progress = pos
                    tvComicPos.text = (pos + 1).toString()
                }

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mPresenter.updateScheduleReadRecord(curPagePos)
                }
            }
        })

        seekComicProc.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {
                //先停止滑动
                rcvOacgComicContent.stopScroll()
                tvComicPosTip.visibility = View.VISIBLE
                tvComicPosTip.text = (curPagePos + 1).toString()
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                rcvOacgComicContent.scrollToPosition(curPagePos)
                mPresenter.updateScheduleReadRecord(curPagePos)
                tvComicPosTip.visibility = View.GONE
            }

            override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    curPagePos = progress
                    val displayPos = (progress + 1).toString()
                    tvComicPosTip.text = displayPos
                    tvComicPos.text = displayPos
                }
            }

        })

        mPresenter.setIntentData(intent.getStringExtra(IntentConstant.OACG_COMIC_ID),
                intent.getStringExtra(IntentConstant.OACG_COMIC_CHAPTERID))
        mPresenter.getEpisodeDetailAndCache()
    }

    @OnClick(R2.id.btn_comic_back)
    fun onBack() {
        finish()
    }

    @OnClick(R2.id.btn_comic_before)
    fun preComicEpisode() {
        rcvOacgComicContent.stopScroll()
        mPresenter.getPreEpisodeDetail()
    }

    @OnClick(R2.id.btn_comic_next)
    fun nextComicEpisode() {
        rcvOacgComicContent.stopScroll()
        mPresenter.getNextEpisodeDetail()
    }

    override fun showEpisodeDetail(episodePage: OacgComicEpisodePage, lastPagePos: Int) {
        layoutComicBottom.visibility = View.VISIBLE
        //是否存在上一话
        if (episodePage.preIndex.toInt() <= 0) {
            btnComicBefore.visibility = View.INVISIBLE
        } else {
            btnComicBefore.visibility = View.VISIBLE
        }
        //是否存在下一话
        if (episodePage.nextIndex.toInt() <= 0) {
            btnComicNext.visibility = View.INVISIBLE
        } else {
            btnComicNext.visibility = View.VISIBLE
        }
        tvComicTitle.text = curComicTitle
        tvComicTitle.append(" ${episodePage.currTitle}")
        oacgComicReadAdapter.setNewData(episodePage.pageContent)
        if (episodePage.pageContent == null || episodePage.pageContent.isEmpty()) {
            maxPage = 0
        } else {
            maxPage = episodePage.pageContent.size
            //每次刷新重置控件
            rcvOacgComicContent.scrollToPosition(lastPagePos)
            seekComicProc.progress = lastPagePos
            seekComicProc.max = maxPage - 1
            tvComicPos.text = (lastPagePos + 1).toString()
        }
        tvComicCount.text = maxPage.toString()
    }

    /**
     * 触发上下栏的显示与隐藏
     */
    internal fun toogleDisplayPanel() {
        if (mVisible) {
            hidePanel()
            mVisible = false
        } else {
            showPanel()
            mVisible = true
        }
    }

    /**
     * 展示上下栏
     */
    internal fun showPanel() {
        val translateAnimation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f)
        val translateAnimation1 = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f)
        translateAnimation.duration = UI_ANIMATION_DELAY.toLong()
        translateAnimation1.duration = UI_ANIMATION_DELAY.toLong()
        layoutComicTop.animation = translateAnimation1
        layoutComicBottom.animation = translateAnimation
        layoutComicTop.visibility = View.VISIBLE//这里通过改变可见性来播放动画
        layoutComicBottom.visibility = View.VISIBLE//这里通过改变可见性来播放动画
    }

    /**
     * 隐藏上下栏
     */
    internal fun hidePanel() {
        val translateAnimation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f)
        val translateAnimation1 = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f)
        translateAnimation.duration = UI_ANIMATION_DELAY.toLong()
        translateAnimation1.duration = UI_ANIMATION_DELAY.toLong()
        layoutComicTop.animation = translateAnimation
        layoutComicBottom.animation = translateAnimation1
        layoutComicTop.visibility = View.INVISIBLE//这里通过改变可见性来播放动画
        layoutComicBottom.visibility = View.INVISIBLE//这里通过改变可见性来播放动画
    }
}