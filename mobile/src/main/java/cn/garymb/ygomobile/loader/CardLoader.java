package cn.garymb.ygomobile.loader;

import android.app.ProgressDialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.garymb.ygomobile.AppsSettings;
import cn.garymb.ygomobile.Constants;
import cn.garymb.ygomobile.lite.R;
import cn.garymb.ygomobile.ui.plus.VUiKit;
import ocgcore.LimitManager;
import ocgcore.data.Card;
import ocgcore.data.LimitList;
import ocgcore.enums.LimitType;
import ocgcore.handler.CardManager;

public class CardLoader implements ICardLoader {
    //    private StringManager mStringManager = StringManager.get();
    private LimitManager mLimitManager = LimitManager.get();
    private Context context;
    private CallBack mCallBack;
    private LimitList mLimitList;
    private static final String TAG = CardLoader.class.getSimpleName();
    private final static boolean DEBUG = false;
    private final CardManager mCardManager;

    public interface CallBack {
        void onSearchStart();

        void onLimitListChanged(LimitList limitList);

        void onSearchResult(List<Card> Cards);

        void onResetSearch();
    }

    public CardLoader(Context context) {
        this.context = context;
        mCardManager = new CardManager(
                AppsSettings.get().getDataBasePath(),
                AppsSettings.get().getExpansionsPath().getAbsolutePath());
    }

    @Override
    public void setLimitList(LimitList limitList) {
        mLimitList = limitList;
        if (mCallBack != null) {
            mCallBack.onLimitListChanged(limitList);
        }
    }

    public Map<Long, Card> readCards(List<Long> ids, LimitList limitList) {
        if (!isOpen()) {
            return null;
        }
        Map<Long, Card> map = new HashMap<>();
        for (Long id : ids) {
            if (id != 0) {
                map.put(id, mCardManager.getCard(id));
            }
        }
        return map;
    }

    public boolean openDb() {
        mCardManager.loadCards();
        return true;
    }

    public boolean isOpen() {
        return mCardManager.getCount() > 0;
    }

    public void setCallBack(CallBack callBack) {
        mCallBack = callBack;
    }

    public void loadData() {
        loadData(null, 0);
    }

    @Override
    public LimitList getLimitList() {
        return mLimitList;
    }

    public Map<Long, Card> readAllCardCodes() {
        if (DEBUG) {
            Map<Long, Card> tmp = new HashMap<>();
            tmp.put(269012L, new Card(269012L).type(524290L));
            tmp.put(27551L, new Card(27551L).type(131076L));
            tmp.put(32864L, new Card(32864L).type(131076L));
            tmp.put(62121L, new Card(62121L).type(131076L));
            tmp.put(135598L, new Card(135598L).type(131076L));
            return tmp;
        } else {
            return mCardManager.getAllCards();
        }
    }

    private void loadData(CardSearchInfo searchInfo, long setcode) {
        if (!isOpen()) {
            return;
        }
        if (Constants.DEBUG)
            Log.i(TAG, "searchInfo=" + searchInfo);
        if (mCallBack != null) {
            mCallBack.onSearchStart();
        }
        ProgressDialog wait = ProgressDialog.show(context, null, context.getString(R.string.searching));
        VUiKit.defer().when(() -> {
            List<Card> tmp = new ArrayList<Card>();
            Map<Long, Card> cards = mCardManager.getAllCards();
            Iterator<Card> cardIterator = cards.values().iterator();
            while (cardIterator.hasNext()) {
                Card card = cardIterator.next();
                if (searchInfo == null || searchInfo.check(card)) {
                    tmp.add(card);
                }
            }
            Collections.sort(tmp, ASC);
            return tmp;
        }).fail((e) -> {
            if (mCallBack != null) {
                mCallBack.onSearchResult(null);
            }
            wait.dismiss();
        }).done((tmp) -> {
            if (mCallBack != null) {
                mCallBack.onSearchResult(tmp);
            }
            wait.dismiss();
        });
    }

    private Comparator<Card> ASC = new Comparator<Card>() {
        @Override
        public int compare(Card o1, Card o2) {
            if (o1.getStar() == o2.getStar()) {
                if (o1.Attack == o2.Attack) {
                    return (int) (o2.Code - o1.Code);
                } else {
                    return o2.Attack - o1.Attack;
                }
            } else {
                return o2.getStar() - o1.getStar();
            }
        }
    };

    @Override
    public void onReset() {
        if (mCallBack != null) {
            mCallBack.onResetSearch();
        }
    }


    @Override
    public void search(String prefixWord, String suffixWord,
                       long attribute, long level, long race,
                       long limitlist, long limit,
                       String atk, String def, long pscale,
                       long setcode, long category, long ot, boolean islink, long... types) {
        CardSearchInfo searchInfo = new CardSearchInfo();
        if (!TextUtils.isEmpty(prefixWord) && !TextUtils.isEmpty(suffixWord)) {
            searchInfo.prefixWord = prefixWord;
            searchInfo.suffixWord = suffixWord;
        } else if (!TextUtils.isEmpty(prefixWord)) {
            searchInfo.word = prefixWord;
        } else if (!TextUtils.isEmpty(suffixWord)) {
            searchInfo.word = suffixWord;
        }
        searchInfo.attribute = (int) attribute;
        searchInfo.level = (int) level;
        searchInfo.atk = atk;
        searchInfo.def = def;
        searchInfo.ot = (int) ot;
        searchInfo.islink = islink;
        searchInfo.types = types;

        searchInfo.category = category;
        searchInfo.race = race;
        searchInfo.pscale = (int) pscale;

        LimitList limitList = mLimitManager.getLimit((int) limitlist);
        LimitType cardLimitType = LimitType.valueOf(limit);
        if (limitList != null) {
            List<Long> ids;
            if (cardLimitType == LimitType.Forbidden) {
                ids = limitList.forbidden;
            } else if (cardLimitType == LimitType.Limit) {
                ids = limitList.limit;
            } else if (cardLimitType == LimitType.SemiLimit) {
                ids = limitList.semiLimit;
            } else if (cardLimitType == LimitType.All) {
                ids = limitList.getCodeList();
            } else {
                ids = null;
            }
            if (ids != null) {
                searchInfo.inCards.addAll(ids);
            }
        }
        setLimitList((limitList == null ? mLimitList : limitList));
        loadData(searchInfo, setcode);
    }
}
