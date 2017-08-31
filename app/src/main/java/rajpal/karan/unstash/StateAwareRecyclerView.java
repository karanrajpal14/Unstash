package rajpal.karan.unstash;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import timber.log.Timber;

public class StateAwareRecyclerView extends RecyclerView {

    private View emptyView;
    final private AdapterDataObserver observer = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            Timber.d("onChanged called");
            checkIfEmpty();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            super.onItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            super.onItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            Timber.d("onIRI called");
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            Timber.d("onIRR called");
            checkIfEmpty();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount);
        }
    };

    public StateAwareRecyclerView(Context context) {
        super(context);
    }

    public StateAwareRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StateAwareRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void checkIfEmpty() {
        if (emptyView == null) {
            Timber.d("EmptyView null");
        }
        if (getAdapter() == null) {
            Timber.d("adapter null");
        }
        if (emptyView != null && getAdapter() != null) {
            final boolean emptyViewVisible = getAdapter().getItemCount() == 0;
            Timber.d("EmptyViewVisibility " + emptyViewVisible);
            int vis1 = emptyViewVisible ? VISIBLE : GONE;
            Timber.d("vis1 " + vis1);
            emptyView.setVisibility(vis1);
            int vis2 = emptyViewVisible ? GONE : VISIBLE;
            Timber.d("vis2 " + vis2);
            setVisibility(vis2);
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        Timber.d("Setting new adapter " + adapter);
        final Adapter oldAdapter = getAdapter();
        Timber.d("old adapter " + oldAdapter);
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(observer);
            super.setAdapter(adapter);
            if (adapter != null) {
                adapter.registerAdapterDataObserver(observer);
            }
            checkIfEmpty();
        }
    }

    public void setEmptyView(View emptyView) {
        Timber.d("Setting empty view");
        this.emptyView = emptyView;
        checkIfEmpty();
    }
}
