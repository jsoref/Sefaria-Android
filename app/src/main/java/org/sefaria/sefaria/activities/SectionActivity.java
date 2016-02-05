package org.sefaria.sefaria.activities;

import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.TextElements.SectionAdapter;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.ListViewExt;

import java.util.ArrayList;
import java.util.List;

public class SectionActivity extends SuperTextActivity implements AbsListView.OnScrollListener, LinkFragment.OnLinkFragInteractionListener {

    private ListViewExt listView;
    private SectionAdapter sectionAdapter;

    private int preLast;
    //text formatting props
    //private boolean isLoadingSection; //to make sure multiple sections don't get loaded at once


    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        if(badOnCreate){
            finish();
            return;
        }
        setContentView(R.layout.activity_section);
        init();
    }

    protected void init() {
        super.init();
        listView = (ListViewExt) findViewById(R.id.listview);
        sectionAdapter = new SectionAdapter(this,R.layout.adapter_text_mono,new ArrayList<Text>());


        listView.setAdapter(sectionAdapter);
        listView.setOnScrollListener(this);
        listView.setDivider(null);

        listView.setOnItemClickListener(onItemClickListener);
        listView.setOnItemLongClickListener(onItemLongClickListener);
        listView.setOnScrollStoppedListener(new ListViewExt.OnScrollStoppedListener() {

            public void onScrollStopped() {
                updateFocusedSegment();
            }
        });

        AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
        als.execute();

        //LINK FRAGMENT
        linkFragment = new LinkFragment();
        //Bundle args = new Bundle();
        //args.putParcelable(LinkFragment.ARG_CURR_SECTION, sectionAdapter.getItem(position));
        //linkFragment.setArguments(args);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.linkRoot, linkFragment);
        fragmentTransaction.commit();




    }



    protected void setTextLang(Util.Lang textLang) {
        this.textLang = textLang;
        sectionAdapter.notifyDataSetChanged();
    }

    protected void setMenuLang(Util.Lang menuLang){
        this.menuLang = menuLang;
        //TODO change the menuItems' lang
    }

    public boolean getIsCts(){ return isCts;}

    protected void setIsCts(boolean isCts) {
        this.isCts = isCts;
        sectionAdapter.notifyDataSetChanged();
    }

    protected void incrementTextSize(boolean isIncrement) {
        float increment = getResources().getDimension(R.dimen.text_font_size_increment);
        if (isIncrement) textSize  += increment;
        else textSize -= increment;

        sectionAdapter.notifyDataSetChanged();
    }

    private void updateFocusedSegment() {
        float mid = ((float)listView.getHeight())/2;
        int numChildren = listView.getChildCount();

        for (int i = 0; i < numChildren; i++) {
            View v = listView.getChildAt(i);
            if (v.getTop() < mid && v.getBottom() > mid) {
                if (linkFragment.getIsOpen()) {
                    int currInd = i + listView.getFirstVisiblePosition();
                    Text currSeg = sectionAdapter.getItem(currInd);
                    if (currSeg.equals(linkFragment.getSegment())) return; //no need to update

                    if (currSeg.isChapter()) {//TODO maybe make this select the chapter links...but not actually
                        currSeg = sectionAdapter.getItem(currInd + 1);
                    }
                    linkFragment.updateFragment(currSeg);
                }
                break;
            }
        }

    }


    @Override
    public void onScroll(AbsListView lw, final int firstVisibleItem,
                         final int visibleItemCount, final int totalItemCount) {

        switch(lw.getId()) {
            case R.id.listview:
                if (!isLoadingSection && !isLoadingInit) {
                    int lastItem = firstVisibleItem + visibleItemCount;
                    if (firstVisibleItem == 0) {
                        AsyncLoadSection als = new AsyncLoadSection(TextEnums.PREV_SECTION);
                        als.execute();
                    }
                    if (lastItem == totalItemCount && preLast != lastItem) {
                        preLast = lastItem;
                        AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
                        als.execute();
                    }

                    Text topSegment = sectionAdapter.getItem(firstVisibleItem);
                    setCurrNode(topSegment);
                }
        }
    }


    @Override
    protected void jumpToText(Text text) {
        Log.d("SectionAct", "calling jump to Text");
        int index = sectionAdapter.getPosition(text);
        Log.d("sec","INDEX " + index);
        listView.setSelection(index);
    }

    //YOU actually need this function implemented because you're implementing AbsListView, but it's stupid...
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //blah...
    }

    ListView.OnItemClickListener onItemClickListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //close textMenuBar if its open
            if (isTextMenuVisible) toggleTextMenu();

            View linkRoot = findViewById(R.id.linkRoot);
            if (linkFragment.getIsOpen()) {

                //linkRoot.setVisibility(View.GONE);
                AnimateLinkFragClose(linkRoot);

            } else {
                if (view.getTop() > 0) //don't auto-scroll if the text is super long.
                    listView.smoothScrollToPositionFromTop(position,0,SuperTextActivity.LINK_FRAG_ANIM_TIME);
                linkFragment.setClicked(true);
                linkFragment.updateFragment(sectionAdapter.getItem(position));
                //linkRoot.setVisibility(View.VISIBLE);
                AnimateLinkFragOpen(linkRoot);




            }

        }
    };

    ListView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d("SectionAct", "long click");
            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            // Creates a new text clip to put on the clipboard
            Text text = sectionAdapter.getItem(position);
            String copiedText;
            if(textLang == Util.Lang.BI)
                copiedText = text.heText + "\n" + text.enText;
            else if(textLang == Util.Lang.EN)
                copiedText = text.enText;
            else //textLang == HE
                copiedText = text.heText;
            ClipData clip = ClipData.newPlainText("Sefaria Text", copiedText);
            // Set the clipboard's primary clip.
            clipboard.setPrimaryClip(clip);
            Toast.makeText(SectionActivity.this, "Copied to Clipboard", Toast.LENGTH_SHORT).show();
            return true;
        }
    };

    public class AsyncLoadSection extends AsyncTask<Void,Void,List<Text>> {

        private TextEnums dir;

        public AsyncLoadSection (TextEnums dir) {
            this.dir = dir;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isLoadingSection = true;
        }

        @Override
        protected List<Text> doInBackground(Void... params) {
            return loadSection(dir);
        }

        @Override
        protected void onPostExecute(List<Text> textsList) {
            isLoadingSection = false;
            isLoadingInit = false;
            if (textsList.size() == 0) return;

            Text sectionHeader = getSectionHeaderText(dir);
            if (dir == TextEnums.NEXT_SECTION) {
                if(sectionHeader.enText.length() > 0 || sectionHeader.heText.length() > 0)
                    sectionAdapter.add(sectionHeader);
                sectionAdapter.addAll(textsList);

            } else if (dir == TextEnums.PREV_SECTION) {
                sectionAdapter.addAll(0, textsList);
                sectionAdapter.add(0, sectionHeader);
                listView.setSelection(textsList.size()+1);
            }

            if (openToText != null) {
                jumpToText(openToText);
                openToText = null;
            }

        }





    }

    /**
     * LINK FRAGMENT
     */

    @Override
    protected void onFinishLinkFragOpen() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.ALIGN_TOP, R.id.useless);
        lp.addRule(RelativeLayout.ABOVE,R.id.linkRoot);

        listView.setLayoutParams(lp);


    }

    @Override
    protected void onStartLinkFragClose() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.ALIGN_TOP,R.id.useless);
        //lp.addRule(RelativeLayout.ABOVE,R.id.linkRoot);

        listView.setLayoutParams(lp);
    }
}
