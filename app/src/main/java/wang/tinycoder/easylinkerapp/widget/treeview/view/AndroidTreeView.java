package wang.tinycoder.easylinkerapp.widget.treeview.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import wang.tinycoder.easylinkerapp.R;
import wang.tinycoder.easylinkerapp.widget.treeview.holder.SimpleViewHolder;
import wang.tinycoder.easylinkerapp.widget.treeview.model.TreeNode;


public class AndroidTreeView {
    private static final String NODES_PATH_SEPARATOR = ";";

    protected TreeNode mRoot;
    private Context mContext;
    private boolean applyForRoot;
    private int containerStyle = 0;
    private Class<? extends TreeNode.BaseNodeViewHolder> defaultViewHolderClass = SimpleViewHolder.class;
    private TreeNode.TreeNodeClickListener nodeClickListener;
    private TreeNode.TreeNodeLongClickListener nodeLongClickListener;
    private boolean mSelectionModeEnabled;
    private boolean mUseDefaultAnimation = false;
    private boolean use2dScroll = false;
    // 自动折叠、展开
    private boolean enableExpansionAutoToggle = true;
    // 自动全选
    private boolean enableSelectionsAutoToggle = true;
    private ViewGroup mRootView;
    private TreeNode currentSelectedLeaf;
    // 自动滑动到展开的view
    private boolean autoScrollToExpandedNode = true;
    // 自动滑动到选中的叶子节点
    private boolean autoScrollToSelectedLeafs = false;

    public AndroidTreeView(Context context) {
        mContext = context;
    }

    public void setRoot(TreeNode mRoot) {
        this.mRoot = mRoot;
    }

    public AndroidTreeView(Context context, TreeNode root) {
        mRoot = root;
        mContext = context;
    }

    public void setAutoScrollToSelectedLeafs(boolean autoScrollToSelectedLeafs) {
        this.autoScrollToSelectedLeafs = autoScrollToSelectedLeafs;
    }

    public void setAutoScrollToExpandedNode(boolean autoScrollToExpandedNode) {
        this.autoScrollToExpandedNode = autoScrollToExpandedNode;
    }

    public void setDefaultAnimation(boolean defaultAnimation) {
        this.mUseDefaultAnimation = defaultAnimation;
    }

    public void setDefaultContainerStyle(int style) {
        setDefaultContainerStyle(style, false);
    }

    public void setDefaultContainerStyle(int style, boolean applyForRoot) {
        containerStyle = style;
        this.applyForRoot = applyForRoot;
    }

    public void setUse2dScroll(boolean use2dScroll) {
        this.use2dScroll = use2dScroll;
    }

    public boolean is2dScrollEnabled() {
        return use2dScroll;
    }

    public void setExpansionAutoToggle(boolean enableAutoToggle) {
        this.enableExpansionAutoToggle = enableAutoToggle;
    }

    public boolean isExpansionAutoToggleEnabled() {
        return enableExpansionAutoToggle;
    }

    public void setLeafSelectionAutoToggle(boolean enableSelectionsAutoToggle) {
        this.enableSelectionsAutoToggle = enableSelectionsAutoToggle;
    }

    public void setDefaultViewHolder(Class<? extends TreeNode.BaseNodeViewHolder> viewHolder) {
        defaultViewHolderClass = viewHolder;
    }

    public void setDefaultNodeClickListener(TreeNode.TreeNodeClickListener listener) {
        nodeClickListener = listener;
    }

    public void setDefaultNodeLongClickListener(TreeNode.TreeNodeLongClickListener listener) {
        nodeLongClickListener = listener;
    }

    public void expandAll() {
        expandNode(mRoot, true);
        if (mRootView instanceof TwoDScrollView) {
            ((TwoDScrollView) mRootView).smoothScrollTo(0, 0);
        } else if (mRootView instanceof ScrollView) {
            ((ScrollView) mRootView).smoothScrollTo(0, 0);
        }
    }

    public void collapseAll() {
        for (TreeNode n : mRoot.getChildren()) {
            collapseNode(n, true);
        }
    }


    public View getView(int style) {
        if (style > 0) {
            ContextThemeWrapper newContext = new ContextThemeWrapper(mContext, style);
            mRootView = use2dScroll ? new TwoDScrollView(newContext) : new ScrollView(newContext);
        } else {
            mRootView = use2dScroll ? new TwoDScrollView(mContext) : new ScrollView(mContext);
        }

        Context containerContext = mContext;
        if (containerStyle != 0 && applyForRoot) {
            containerContext = new ContextThemeWrapper(mContext, containerStyle);
        }
        final LinearLayout viewTreeItems = new LinearLayout(containerContext, null, containerStyle);

        viewTreeItems.setId(R.id.tree_items);
        viewTreeItems.setOrientation(LinearLayout.VERTICAL);
        mRootView.addView(viewTreeItems);

        mRoot.setViewHolder(new TreeNode.BaseNodeViewHolder(mContext) {
            @Override
            public View createNodeView(TreeNode node, Object value) {
                return null;
            }

            @Override
            public ViewGroup getNodeItemsView() {
                return viewTreeItems;
            }
        });
        expandNode(mRoot, false);
        return mRootView;
    }

    public View getView() {
        return getView(-1);
    }


    public void expandLevel(int level) {
        for (TreeNode n : mRoot.getChildren()) {
            expandLevel(n, level);
        }
    }

    private void expandLevel(TreeNode node, int level) {
        boolean lastAutoScrollEnabled = autoScrollToExpandedNode;
        autoScrollToExpandedNode = false;
        if (node.getLevel() <= level) {
            expandNode(node, false);
        }
        for (TreeNode n : node.getChildren()) {
            expandLevel(n, level);
        }
        autoScrollToExpandedNode = lastAutoScrollEnabled;
    }

    public void expandNode(TreeNode node) {
        expandNode(node, false);
        if (node != mRoot) {
            if ((node.isLeaf() && autoScrollToSelectedLeafs) ||
                    (node.isBranch() && autoScrollToExpandedNode)) {
                scrollToNode(node);
            }
        }
    }

    public void collapseNode(TreeNode node) {
        collapseNode(node, false);
    }

    public String getSaveState() {
        final StringBuilder builder = new StringBuilder();
        getSaveState(mRoot, builder);
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    public void restoreState(String saveState) {
        if (!TextUtils.isEmpty(saveState)) {
            collapseAll();
            final String[] openNodesArray = saveState.split(NODES_PATH_SEPARATOR);
            final Set<String> openNodes = new HashSet<>(Arrays.asList(openNodesArray));
            restoreNodeState(mRoot, openNodes);
        }
    }

    private void restoreNodeState(TreeNode node, Set<String> openNodes) {
        for (TreeNode n : node.getChildren()) {
            if (openNodes.contains(n.getPath())) {
                expandNode(n);
                restoreNodeState(n, openNodes);
            }
        }
    }

    private void getSaveState(TreeNode root, StringBuilder sBuilder) {
        for (TreeNode node : root.getChildren()) {
            if (node.isExpanded()) {
                sBuilder.append(node.getPath());
                sBuilder.append(NODES_PATH_SEPARATOR);
                getSaveState(node, sBuilder);
            }
        }
    }

    public void toggleNodeExpansion(TreeNode node) {
        if (node.isExpanded()) {
            collapseNode(node, false);
        } else {
            expandNode(node, false);
            if (node != mRoot) {
                if ((node.isLeaf() && autoScrollToSelectedLeafs) ||
                        (node.isBranch() && autoScrollToExpandedNode)) {
                    scrollToNode(node);
                }
            }
        }
    }

    private void toggleLeafSelection(TreeNode node) {
        if (node.isLeaf()) {
            if (currentSelectedLeaf != null) {
                selectNode(currentSelectedLeaf, false);
            }
            selectNode(node, !node.isSelected());
        }
    }

    private void collapseNode(TreeNode node, final boolean includeSubnodes) {
        node.setExpanded(false);
        TreeNode.BaseNodeViewHolder nodeViewHolder = getViewHolderForNode(node);

        if (mUseDefaultAnimation) {
            collapse(nodeViewHolder.getNodeItemsView());
        } else {
            nodeViewHolder.getNodeItemsView().setVisibility(View.GONE);
        }
        nodeViewHolder.toggle(false);
        if (includeSubnodes) {
            for (TreeNode n : node.getChildren()) {
                collapseNode(n, includeSubnodes);
            }
        }
    }

    private void expandNode(final TreeNode node, boolean includeSubnodes) {
        node.setExpanded(true);
        final TreeNode.BaseNodeViewHolder parentViewHolder = getViewHolderForNode(node);
        parentViewHolder.getNodeItemsView().removeAllViews();


        parentViewHolder.toggle(true);

        for (final TreeNode n : node.getChildren()) {
            addNode(parentViewHolder.getNodeItemsView(), n, -1);
            if (n.isExpanded() || includeSubnodes) {
                expandNode(n, includeSubnodes);
            }
        }

        if (mUseDefaultAnimation) {
            expand(parentViewHolder.getNodeItemsView());
        } else {
            parentViewHolder.getNodeItemsView().setVisibility(View.VISIBLE);
            parentViewHolder.getNodeItemsView().getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
            parentViewHolder.getNodeItemsView().requestLayout();
        }
    }

    public void expandNodeIncludingParents(TreeNode node, boolean autoScroll) {
        List<TreeNode> parents = getParents(node);
        for (TreeNode parentNode : parents) {
            boolean lastAutoScrollEnabled = autoScrollToExpandedNode;
            autoScrollToExpandedNode = false;
            expandNode(parentNode);
            autoScrollToExpandedNode = lastAutoScrollEnabled;
        }
        if (autoScroll) {
            scrollToNode(node);
        }
    }

    private List<TreeNode> getParents(TreeNode node) {
        List<TreeNode> parents = new ArrayList<>();
        TreeNode parent = node;
        while (parent != mRoot) {
            parents.add(0, parent);
            parent = parent.getParent();
        }
        return parents;
    }

    public void scrollToNode(final TreeNode node) {
        if (node.isInitialized()) {
            if (node.getView().getHeight() == 0) {
                node.getView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        node.getView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        scrollToNode(node);
                    }
                });
                return;
            }
            int yToScroll = ((int) node.getView().getY());
            Logger.i("@------@", "selfY : " + yToScroll);
            ViewGroup parent = ((ViewGroup) node.getView().getParent());
            while (parent != mRootView) {
                yToScroll += parent.getY();
                parent = ((ViewGroup) parent.getParent());
            }
            Logger.i("@------@", "allScrollY : " + yToScroll);
            if (mRootView instanceof TwoDScrollView) {
                ((TwoDScrollView) mRootView).smoothScrollTo(0, yToScroll);
            } else if (mRootView instanceof ScrollView) {
                ((ScrollView) mRootView).smoothScrollTo(0, yToScroll);
            }
        }
    }


    private void addNode(ViewGroup container, final TreeNode n, int index) {
        final TreeNode.BaseNodeViewHolder viewHolder = getViewHolderForNode(n);
        final View nodeView = viewHolder.getView();
        if (index == -1) {
            container.addView(nodeView);
        } else {
            container.addView(nodeView, index);
        }

        if (mSelectionModeEnabled) {
            viewHolder.toggleSelectionMode(mSelectionModeEnabled);
        }

        nodeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAll(n);
                if (n.getClickListener() != null) {
                    n.getClickListener().onClick(n, n.getValue());
                } else if (nodeClickListener != null) {
                    nodeClickListener.onClick(n, n.getValue());
                }
            }
        });

        nodeView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                toggleAll(n);
                if (n.getLongClickListener() != null) {
                    return n.getLongClickListener().onLongClick(n, n.getValue());
                } else if (nodeLongClickListener != null) {
                    return nodeLongClickListener.onLongClick(n, n.getValue());
                }
                return false;
            }
        });
    }


    public void toggleAll(TreeNode node) {
        if (enableExpansionAutoToggle) {
            toggleNodeExpansion(node);
        }
        if (enableSelectionsAutoToggle) {
            toggleLeafSelection(node);
        }
    }

    //------------------------------------------------------------
    //  Selection methods

    public void setSelectionModeEnabled(boolean selectionModeEnabled) {
        if (!selectionModeEnabled) {
            // TODO fix double iteration over tree
            deselectAll();
        }
        mSelectionModeEnabled = selectionModeEnabled;

        for (TreeNode node : mRoot.getChildren()) {
            toggleSelectionMode(node, selectionModeEnabled);
        }

    }

    public <E> List<E> getSelectedValues(Class<E> clazz) {
        List<E> result = new ArrayList<>();
        List<TreeNode> selected = getSelected();
        for (TreeNode n : selected) {
            Object value = n.getValue();
            if (value != null && value.getClass().equals(clazz)) {
                result.add((E) value);
            }
        }
        return result;
    }

    public boolean isSelectionModeEnabled() {
        return mSelectionModeEnabled;
    }

    private void toggleSelectionMode(TreeNode parent, boolean mSelectionModeEnabled) {
        toogleSelectionForNode(parent, mSelectionModeEnabled);
        if (parent.isExpanded()) {
            for (TreeNode node : parent.getChildren()) {
                toggleSelectionMode(node, mSelectionModeEnabled);
            }
        }
    }

    public List<TreeNode> getSelected() {
        if (mSelectionModeEnabled) {
            return getSelected(mRoot);
        } else {
            return new ArrayList<>();
        }
    }

    // TODO Do we need to go through whole tree? Save references or consider collapsed nodes as not selected
    private List<TreeNode> getSelected(TreeNode parent) {
        List<TreeNode> result = new ArrayList<>();
        for (TreeNode n : parent.getChildren()) {
            if (n.isSelected()) {
                result.add(n);
            }
            result.addAll(getSelected(n));
        }
        return result;
    }

    public void selectAll(boolean skipCollapsed) {
        makeAllSelection(true, skipCollapsed);
    }

    public void deselectAll() {
        makeAllSelection(false, false);
    }

    private void makeAllSelection(boolean selected, boolean skipCollapsed) {
        if (mSelectionModeEnabled) {
            for (TreeNode node : mRoot.getChildren()) {
                selectNode(node, selected, skipCollapsed);
            }
        }
    }

    public void selectNode(TreeNode node, boolean selected) {
        if (mSelectionModeEnabled) {

            if (node.isLeaf()) {
                if (selected) {
                    currentSelectedLeaf = node;
                } else if (node == currentSelectedLeaf) {
                    currentSelectedLeaf = null;
                }
            }

            node.setSelected(selected);
            toogleSelectionForNode(node, true);
        }
    }

    private void selectNode(TreeNode parent, boolean selected, boolean skipCollapsed) {
        parent.setSelected(selected);
        toogleSelectionForNode(parent, true);
        boolean toContinue = skipCollapsed ? parent.isExpanded() : true;
        if (toContinue) {
            for (TreeNode node : parent.getChildren()) {
                selectNode(node, selected, skipCollapsed);
            }
        }
    }

    private void toogleSelectionForNode(TreeNode node, boolean makeSelectable) {
        TreeNode.BaseNodeViewHolder holder = getViewHolderForNode(node);
        if (holder.isInitialized()) {
            getViewHolderForNode(node).toggleSelectionMode(makeSelectable);
        }
    }

    private TreeNode.BaseNodeViewHolder getViewHolderForNode(TreeNode node) {
        TreeNode.BaseNodeViewHolder viewHolder = node.getViewHolder();
        if (viewHolder == null) {
            try {
                final Object object = defaultViewHolderClass.getConstructor(Context.class).newInstance(mContext);
                viewHolder = (TreeNode.BaseNodeViewHolder) object;
                node.setViewHolder(viewHolder);
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate class " + defaultViewHolderClass);
            }
        }
        if (viewHolder.getContainerStyle() <= 0) {
            viewHolder.setContainerStyle(containerStyle);
        }
        if (viewHolder.getTreeView() == null) {
            viewHolder.setTreeViev(this);
        }
        return viewHolder;
    }

    private static void expand(final View v) {
        v.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? LinearLayout.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    private static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    //-----------------------------------------------------------------
    //Add / Remove

    public void addNode(TreeNode parent, final TreeNode nodeToAdd) {
        addNode(parent, nodeToAdd, -1);
    }

    public void addNode(TreeNode parent, final TreeNode nodeToAdd, int position) {
        parent.addChild(nodeToAdd, position);
        if (parent.isExpanded()) {
            final TreeNode.BaseNodeViewHolder parentViewHolder = getViewHolderForNode(parent);
            addNode(parentViewHolder.getNodeItemsView(), nodeToAdd, position);
        }
    }


    public void removeNode(TreeNode node) {
        if (node.getParent() != null) {
            TreeNode parent = node.getParent();
            int index = parent.deleteChild(node);
            if (parent.isExpanded() && index >= 0) {
                final TreeNode.BaseNodeViewHolder parentViewHolder = getViewHolderForNode(parent);
                parentViewHolder.getNodeItemsView().removeViewAt(index);
            }
        }
    }
}
