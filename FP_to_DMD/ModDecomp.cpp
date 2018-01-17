#include "ModDecomp.h"

// These functions provide begin and end implementations for the BGL iterator
// pairs. Thus we can simply use for(auto e : edges(g)) {} instead of messy
// iterator syntax.
namespace std
{
    template <typename A> A begin(const pair<A, A>& s)
    {
        return s.first;
    }
    
    template <typename A> A end(const pair<A, A>& s)
    {
        return s.second;
    }
}


//DEBUG
void printTree(const Tree& fractureTree, bool toDelete = false, bool name = false, bool corrV = false)
{
    typedef std::map<TVertex, int> IndexMap;
    IndexMap mapIndex;
    boost::associative_property_map<IndexMap> propmapIndex(mapIndex);
    tvertex_iter vi, vi_end;
    int i = 0; // index
    for (boost::tie(vi,vi_end) = boost::vertices(fractureTree); vi != vi_end; ++vi)
        boost::put(propmapIndex,*vi,i++);
    if (toDelete)
        boost::write_graphviz(std::cout, fractureTree, boost::make_label_writer(boost::get(&vertex_prop::toDelete,fractureTree)), boost::default_writer(), boost::default_writer(), propmapIndex);
    else if (name)
        boost::write_graphviz(std::cout, fractureTree, boost::make_label_writer(boost::get(&vertex_prop::name,fractureTree)), boost::default_writer(), boost::default_writer(), propmapIndex);
    else if (corrV)
        boost::write_graphviz(std::cout, fractureTree, boost::make_label_writer(boost::get(&vertex_prop::corrV,fractureTree)), boost::default_writer(), boost::default_writer(), propmapIndex);
    else
        boost::write_graphviz(std::cout, fractureTree, boost::make_label_writer(boost::get(&vertex_prop::type,fractureTree)), boost::default_writer(), boost::default_writer(), propmapIndex);
}

//DEBUG
void printGraph(const Graph& g)
{
    boost::write_graphviz(std::cout, g);
}

template<typename T>
int findFractures(T iter, T siter, const std::vector<std::string>& cutset, const Graph& graph, std::map<Vertex, int>& lbrackets, std::map<Vertex, int>& rbrackets) {
    
    const auto& name = boost::get(boost::vertex_name, graph);
    const auto& facPos = boost::get(facPos_t(), graph);
    
    for (; iter != siter; ++iter) // iter != siter: we cannot be a left fracture if right of pair
    {
        if (std::binary_search(cutset.begin(), cutset.end(), boost::get(name,*iter)))
        {
            //left fracture found
            //insert closing bracket right of siter
            ++rbrackets[*siter];
            ++lbrackets[*iter];
            
            return boost::get(facPos,*iter);
        }
    }
    
    return -1;
}

// Given a factorizing permutation constructs the dyck word (parenthesized factorization) depending on the graph
DyckWord ModDecomp::parenthesizing(const std::vector<Vertex>& fac, const Graph& graph, std::map<Vertex, int>& lcutters, std::map<Vertex, int>& rcutters) // make sure the graph names are the names appearing in the string
{
    const auto& name = boost::get(boost::vertex_name, graph);
    
    // we count for every string opening and closing brackets before/after vertices to build the dyck word from it later on
    std::map<Vertex, int> lbrackets;
    std::map<Vertex, int> rbrackets;
    
    for(size_t i = 0; i < fac.size() - 1; ++i) {
        rcutters[fac[i]] = -1;
        lcutters[fac[i]] = fac.size() + 1;
        
        // Outgoing vertices
        std::vector<std::string> out_nh1;
        std::cout << i << ":" << std::endl << "Out-nh1: ";
        for(const auto v : boost::adjacent_vertices(fac[i], graph)) {
            out_nh1.push_back(boost::get(name, v));
            std::cout << v << " ";
        }
        std::cout << std::endl << "Out-nh2: ";
        
        std::vector<std::string> out_nh2;
        for(const auto v : boost::adjacent_vertices(fac[i + 1], graph)) {
            out_nh2.push_back(boost::get(name, v));
            std::cout << v << " ";
        }
        std::cout << std::endl;
        
        std::sort(out_nh1.begin(), out_nh1.end());
        std::sort(out_nh2.begin(), out_nh2.end());
        std::vector<std::string> out_cutset;
        
        std::set_symmetric_difference(
                                      out_nh1.begin(), out_nh1.end(),
                                      out_nh2.begin(), out_nh2.end(),
                                      std::back_inserter(out_cutset)
                                      );
        std::cout << "out_cutset: ";
        for(auto s : out_cutset){
            std::cout << s << " ";
        }
        std::cout << std::endl;
        
        // F.L. 11.12.17: Incoming vertices???
        std::vector<std::string> in_nh1;
        std::cout << "In-nh1: ";
        for(const auto v : boost::inv_adjacent_vertices(fac[i], graph)) {
            in_nh1.push_back(boost::get(name, v));
            std::cout << v << " ";
        }
        std::cout << std::endl << "In-nh2: ";
        
        std::vector<std::string> in_nh2;
        for(const auto v : boost::inv_adjacent_vertices(fac[i + 1], graph)) {
            in_nh2.push_back(boost::get(name, v));
            std::cout << v << " ";
        }
        std::cout << std::endl;
        
        std::sort(in_nh1.begin(), in_nh1.end());
        std::sort(in_nh2.begin(), in_nh2.end());
        std::vector<std::string> in_cutset;

        std::set_symmetric_difference(
                                      in_nh1.begin(), in_nh1.end(),
                                      in_nh2.begin(), in_nh2.end(),
                                      std::back_inserter(in_cutset)
                                      );
        std::cout << "in_cutset: ";
        for(auto s : in_cutset){
            std::cout << s << " ";
        }
        std::cout << std::endl;
        
        // ... cutset without doubles
        std::vector<std::string> cutset;
        std::set_union(
                       out_cutset.begin(), out_cutset.end(),
                       in_cutset.begin(), in_cutset.end(),
                       std::back_inserter(cutset)
                       );
        std::sort(cutset.begin(), cutset.end());
        
        //std::vector<std::string> cutset = out_cutset;
        std::cout << "cutset: ";
        for(auto s : cutset){
            std::cout << s << " ";
        }
        std::cout << std::endl;

        
        int r = findFractures(fac.begin(), fac.begin() + i, cutset, graph, lbrackets, rbrackets);
        
        // small hack so we can use the same function for l and r brackets
        if(r != -1) {
            lcutters[fac[i]] = r;
        }
        
        rcutters[fac[i]] = findFractures(fac.rbegin(), fac.rbegin() + fac.size() - i - 2, cutset, graph, rbrackets, lbrackets);
    }
    
    rcutters[fac[fac.size() - 1]] = -1;
    lcutters[fac[fac.size() - 1]] = fac.size() + 1;
    // The last vertex does not have a pair and thus does not have any cutters whatsoever
    
    //Now build the dyck word
    return DyckWord(graph, lbrackets, rbrackets, fac);
}

// add child to node curr with lfrac size
TVertex addChild(const TVertex& curr, int size, Tree& fractureTree)
{
    TVertex v = boost::add_vertex(fractureTree);
    fractureTree[v].name = "Internal"; // These will later on become the module nodes [series/parallel/order/prime]
    fractureTree[v].lfrac = size;
    fractureTree[v].rfrac = -1;
    fractureTree[v].toDelete = false; // we need to initalize this, might be set to "true" in "leaveChild"
    fractureTree[v].root = false;
    boost::add_edge(curr,v,fractureTree);
    return v;
}

// does internal vertex curr has to be deleted
bool deleteable(TVertex curr, Vertex lastChild, const Graph& g, const Tree& fractureTree)
{
    const auto& facPos = boost::get(facPos_t(), g);
    return !fractureTree[curr].toDelete and (fractureTree[curr].rfrac > boost::get(facPos,lastChild) or fractureTree[curr].lfrac < boost::get(facPos,fractureTree[curr].containedNodes[0]));
}

// recurr back to father node of curr as curr has been finished
TVertex leaveChild(const TVertex& curr, const DyckWord& dyck, int i, const Graph& g, const std::map<Vertex, int>& lcutters, const std::map<Vertex, int>& rcutters, Tree& fractureTree)
{
    const auto& facPos = boost::get(facPos_t(), g);
    
    int pos = fractureTree[curr].containedNodes.size();
    Vertex lastChild = fractureTree[curr].containedNodes[pos - 1];
    if (boost::out_degree(curr,fractureTree) == 1) // we only have one child: dummy node
        fractureTree[curr].toDelete = true;
    
    const auto& name = boost::get(boost::vertex_name, g);
    fractureTree[curr].corrV = boost::get(name, fractureTree[curr].containedNodes[0]); // initialise
    fractureTree[curr].rfrac = std::max(boost::get(facPos,lastChild), fractureTree[curr].rfrac); // we can only set this now
    
    if (deleteable(curr, lastChild, g, fractureTree))
        fractureTree[curr].toDelete = true; // this node is a dummy node
    
    int ltemp = fractureTree[curr].lfrac;
    int rtemp = fractureTree[curr].rfrac;
    std::vector<Vertex> nodes = fractureTree[curr].containedNodes;
    
    TVertex newCurr = boost::source(*(boost::in_edges(curr,fractureTree).first),fractureTree); // Assume dyck word is well-formed
    // we leave this node for good, give its cutters to parent node
    fractureTree[newCurr].containedNodes.insert(fractureTree[newCurr].containedNodes.end(), nodes.begin(), nodes.end()); // save all the contained Nodes, even if they're not direct children
    if (i < dyck.size() and dyck.getToken(i + 1).getType() != Type::RBracket) // If we do not leave the node in the next step  we need to consider the cutters of the last child
    {
        fractureTree[newCurr].lfrac = std::min(std::min(lcutters.at(lastChild), fractureTree[newCurr].lfrac), ltemp); // leftmost cutter
        fractureTree[newCurr].rfrac = std::max(std::max(rcutters.at(lastChild), fractureTree[newCurr].rfrac), rtemp); // rightmost cutter
    }
    else
    { // this was really the last node, we must not consider the cutters of the last child
        fractureTree[newCurr].lfrac = std::min(fractureTree[newCurr].lfrac, ltemp); // leftmost cutter
        fractureTree[newCurr].rfrac = std::max(fractureTree[newCurr].rfrac, rtemp); // rightmost cutter
    }
    
    return newCurr;
}

// a vertex corresponding to the original graph has been found, add as leaf
void addLeaf(const TVertex& curr, const Token& token, const DyckWord& dyck, int i, Graph& g, const std::map<Vertex, int>& lcutters, const std::map<Vertex, int>& rcutters, Tree& fractureTree)
{
    const auto& cv = boost::get(corrV_t(), g); // this is not really the color but it does work [stores the corresponding vertex in the graph]
    const auto& facPos = boost::get(facPos_t(), g);
    const auto& name = boost::get(boost::vertex_name, g);
    
    TVertex leaf = boost::add_vertex(fractureTree);
    
    Vertex v = token.getVertex();
    
    fractureTree[leaf].name = token.toString(g);
    fractureTree[leaf].type = token.toString(g); // type in Modular Decomposition = name in original graph
    fractureTree[leaf].lfrac = dyck.size() + 1;
    fractureTree[leaf].rfrac = -1;
    fractureTree[leaf].toDelete = false; // if this is uninitialized, strange things may happen
    fractureTree[leaf].root = false;
    fractureTree[leaf].corrV = boost::get(name,v); // go over name
    fractureTree[leaf].containedNodes = std::vector<Vertex>{v};
    boost::put(cv, v, leaf);
    
    fractureTree[curr].children.push_back(v); // direct children
    fractureTree[curr].containedNodes.push_back(v); //we need this for module detection
    
    boost::add_edge(curr,leaf,fractureTree);
    //
    if (boost::out_degree(curr,fractureTree) == 1) // we're the first node added
        fractureTree[curr].lfrac = std::min(boost::get(facPos,v),std::min(fractureTree[curr].lfrac, lcutters.at(v))); //is firstChild the minimum?
    
    if (dyck.getToken(i + 1).getType() != Type::RBracket) //iter + 1 always exists as the last node is always ")". This means, we are not the last vertex added
    {
        fractureTree[curr].lfrac = std::min(fractureTree[curr].lfrac, lcutters.at(v)); // leftmost cutter, we now can leave firstChild out
        fractureTree[curr].rfrac = std::max(fractureTree[curr].rfrac, rcutters.at(v)); // rightmost cutter
    }
    // If we are the last vertex added: we must not consider fractures going "out" of the module
}

// Given the Dyck word builds the fracture tree
Tree ModDecomp::buildFractureTree(const DyckWord& dyck, Graph& g, const std::map<Vertex, int>& lcutters, const std::map<Vertex, int>& rcutters)
{
    // create root
    Tree fractureTree;
    TVertex root = boost::add_vertex(fractureTree);
    fractureTree[root].name = "Internal";
    fractureTree[root].root = true;
    fractureTree[root].toDelete = false;
    TVertex curr = root;
    for (int i = 0; i < dyck.size(); i++){
        Token t = dyck.getToken(i);
        switch (t.getType())
        {
            case Type::LBracket : curr = addChild(curr, dyck.size() + 1, fractureTree); break;
            case Type::RBracket : curr = leaveChild(curr, dyck, i, g, lcutters, rcutters, fractureTree); break;
            case Type::Node : addLeaf(curr, t, dyck, i, g, lcutters, rcutters, fractureTree); break;
        }
    }
    if (boost::out_degree(root,fractureTree) == 1)
        fractureTree[root].toDelete = true; // todo: there might be more.
    return fractureTree;
}

// delete the marked vertex v
void deleteVertex(TVertex& v, Tree& fractureTree)
{
    auto parent = *boost::inv_adjacent_vertices(v, fractureTree).first;
    
    std::vector<TEdge> toDelete;
    for (auto ei : boost::out_edges(v, fractureTree)) // remove all edges from node
    {
        boost::add_edge(parent, boost::target(ei,fractureTree), fractureTree); // link children to parent of vertex to be removed
        toDelete.push_back(ei);
    }
    
    for (const auto& e : toDelete)
        boost::remove_edge(e, fractureTree);
    
    fractureTree[parent].children.insert(
                                         fractureTree[parent].children.end(),
                                         fractureTree[v].children.begin(),
                                         fractureTree[v].children.end()); // add direct children to father
    
    // The children should be in "contained Nodes" anyways
    boost::remove_edge(parent,v,fractureTree); // remove edge to node
    boost::remove_vertex(v,fractureTree); // all edges are already gone
}

// Detects all modules and deletes dummy nodes which do not represent a module
void moduleDetDel(Tree& fractureTree) //Detect modules, delete dummies
{ // This craps on my name mapping
    // Delete all nodes which flags have been set in the previous step
    tvertex_iter vi, vi_end, next;
    boost::tie(vi,vi_end) = boost::vertices(fractureTree);
    for (next = vi; vi != vi_end; vi = next)
    {
        ++next;
        if (fractureTree[*vi].toDelete && !fractureTree[*vi].root) // we cant do this if we are root (link to parents)
        {
            std::cout << "Deleting node: " << fractureTree[*vi].type << std::endl;
            deleteVertex(*vi, fractureTree);
        }
        else if (fractureTree[*vi].toDelete && fractureTree[*vi].root) // this is only the case iff root has exactly one child. Otherwise the root is always a module: The one containing all nodes
        {
            std::cout << "Deleting root: " << fractureTree[*vi].type << std::endl;
            
            auto child = *boost::adjacent_vertices(*vi, fractureTree).first;
            
            fractureTree[child].root = true; // we can do this
            boost::remove_edge(*(boost::out_edges(*vi,fractureTree).first),fractureTree); // remove _the_ out edge (can only be one)
            // we dont link child to parent because root does not have parent
            boost::remove_vertex(*vi,fractureTree); // there are no in-edges so we can delete the old root now
        }
    }
}

// not really LCA, get direct child of v which is an ancestor of curr TODO: does this work for me???
TVertex getLCA(const TVertex& curr, const TVertex& v, const Tree& fractureTree)
{
    TVertex tmp = curr;
    while (*boost::inv_adjacent_vertices(tmp,fractureTree).first != v)
        tmp = *boost::inv_adjacent_vertices(tmp,fractureTree).first;
    return tmp;
}

// Todo: how does this work?
// merging DOES occur, find the modules to merge
bool getOriginalModule(const SubGraph& rootG, const std::vector<Vertex>& fac, const TVertex& v, const Vertex& v1, const Vertex& v2, TVertex& tA1, TVertex& tA2, const std::map<Vertex, int>& lcutters, const std::map<Vertex, int>& rcutters, Tree& fractureTree)
{
    const auto& cv = boost::get(corrV_t(), rootG);
    const auto& facPos = boost::get(facPos_t(), rootG);
    
    // real children of *ti?
    bool child1 = std::find(fractureTree[v].children.begin(), fractureTree[v].children.end(),v1) != fractureTree[v].children.end();
    bool child2 = std::find(fractureTree[v].children.begin(), fractureTree[v].children.end(),v2) != fractureTree[v].children.end();
    
    TVertex curr1 = boost::get(cv,v1);
    TVertex curr2 = boost::get(cv,v2);
    bool rep = false;
    int posF;
    int posL;
    
    // which of the two vertices is a "real" child of the module = leaf as direct child
    if (child1 and !child2)
    {
        curr2 = getLCA(curr2,v,fractureTree);
        posF = lcutters.at(v1);
        posL = rcutters.at(v1);// only v1 is a real child should not have any cutters with v2
        rep = (posF >= boost::get(facPos,v2) and posL <= boost::get(facPos,*(fractureTree[curr2].containedNodes.rbegin())));
    }
    else if (child2 and !child1)
    {
        curr1 = getLCA(curr1,v,fractureTree);
        Vertex lastVertex = *(fractureTree[curr1].containedNodes.rbegin());
        posF = lcutters.at(lastVertex);
        posL = rcutters.at(lastVertex);
        rep = (posF >= boost::get(facPos,*(fractureTree[curr1].containedNodes.begin())) and posL <= boost::get(facPos,v2));
    }
    else if (!child1 and !child2)
    {
        curr1 = getLCA(curr1,v,fractureTree);
        curr2 = getLCA(curr2,v,fractureTree);
        Vertex lastVertex = *(fractureTree[curr1].containedNodes.rbegin());
        posF = lcutters.at(lastVertex);
        posL = rcutters.at(lastVertex);
        rep = (posF >= boost::get(facPos,*(fractureTree[curr1].containedNodes.begin())) and posL <= boost::get(facPos,*(fractureTree[curr2].containedNodes.rbegin())));
    }
    else
    {
        posF = lcutters.at(v1);
        posL = rcutters.at(v1); // both are real children, left and right cutter between v1 and v2 must not exist
        rep = false; // just to be sure that posF = fac.size() +1 and posL = -1 has to be true
    }
    
    bool fc = (posF == fac.size() + 1);
    bool lc = (posL == -1);
    
    // set vertices which have to be added/merged
    if (curr1 != curr2 and ((fc and lc) or rep))
    {
        tA1 = curr1;
        tA2 = curr2;
        return true;
    }
    
    return false;
}

// we have detected a case where merging might occur/module type has to be assigned
void detectMerging(const SubGraph& rootG, const Graph& graph, const TVertex& v, const std::vector<Vertex>& fac, int minPos, int maxPos, const std::map<Vertex, int>& lcutters, const std::map<Vertex, int>& rcutters, Tree& fractureTree)
{
    // temporaries
    const auto& cv = boost::get(corrV_t(), rootG);
    bool added = false;
    TVertex merged;
    std::vector<TVertex> newModules;
    std::vector<std::pair<TVertex,TVertex> > toDelete;
    std::vector<std::pair<TVertex,TVertex> > toAdd;
    
    
    for (std::vector<Vertex>::const_iterator it = fac.begin() + minPos; it != fac.begin() + maxPos; ++it) // my beloved paris [sic!]
    {
        bool foundNew = false;
        
        TVertex tA1;
        TVertex tA2;
        
        foundNew = getOriginalModule(rootG, fac, v, *it, *(it+1), tA1, tA2, lcutters, rcutters, fractureTree); // true if merging occurs
        
        if (foundNew)
        {
            if (!added)
            {
                merged = boost::add_vertex(fractureTree);
                fractureTree[merged].name = "Internal"; // we do not necessarily need to initalize the other values
                fractureTree[merged].root = false;
                
                // F.L.: For undirected, one edge was enough. I need to check for both directions.
                Vertex fst = *it;
                Vertex snd = *(it+1);
                bool fromEdge = boost::edge(*it,*(it + 1),graph).second;
                bool toEdge   = boost::edge(*(it + 1),*it,graph).second;
                if (fromEdge && toEdge)
                    fractureTree[merged].type = "Series";
                else if(!fromEdge && !toEdge)
                    fractureTree[merged].type = "Parallel";
                else
                    fractureTree[merged].type = "Order";
                std::cout << "(" << fst << "," << snd << "): merged " << fractureTree[merged].type << std::endl;
                //Add contained nodes
                if (fractureTree[tA1].name == "Internal")
                    fractureTree[merged].containedNodes.insert(fractureTree[merged].containedNodes.end(), fractureTree[tA1].containedNodes.begin(), fractureTree[tA1].containedNodes.end());
                else
                {
                    fractureTree[merged].children.insert(fractureTree[merged].children.begin(), fractureTree[tA1].children.begin(), fractureTree[tA1].children.end());
                    fractureTree[merged].containedNodes.insert(fractureTree[merged].containedNodes.begin(), fractureTree[tA1].containedNodes.begin(), fractureTree[tA1].containedNodes.end());
                }
                toAdd.push_back(std::make_pair(merged,tA1));
                toDelete.push_back(std::make_pair(v,tA1));
                newModules.push_back(merged);
                added = true;
            }
            //boost::add_edge(merged,t2,fractureTree);
            if (fractureTree[tA2].name == "Internal")
                fractureTree[merged].containedNodes.insert(fractureTree[merged].containedNodes.end(), fractureTree[tA2].containedNodes.begin(), fractureTree[tA2].containedNodes.end());
            else
            {
                fractureTree[merged].children.insert(fractureTree[merged].children.begin(), fractureTree[tA2].children.begin(), fractureTree[tA2].children.end());
                fractureTree[merged].containedNodes.insert(fractureTree[merged].containedNodes.begin(), fractureTree[tA2].containedNodes.begin(), fractureTree[tA2].containedNodes.end());
            }
            toAdd.push_back(std::make_pair(merged,tA2));
            toDelete.push_back(std::make_pair(v,tA2));
            foundNew = false;
        }
        else if (getLCA(boost::get(cv,*it),v, fractureTree) != getLCA(boost::get(cv,*(it+1)),v,fractureTree))
            added = false;
    }
    // add merged module
    for (std::vector<std::pair<TVertex, TVertex> >::iterator it = toAdd.begin(); it != toAdd.end(); ++it)
        boost::add_edge(it->first, it->second, fractureTree);
    
    for (std::vector<std::pair<TVertex, TVertex> >::iterator it = toDelete.begin(); it != toDelete.end(); ++it)
        boost::remove_edge(it->first,it->second,fractureTree);
    
    for (const auto& vert : newModules)
        boost::add_edge(v, vert, fractureTree);
    
}

// does the calculation for module assigning
void calculateModuleType(const Graph& graph, SubGraph& tmp, const TVertex& v, const std::vector<Vertex>& fac, int minPos, int maxPos, const std::map<Vertex, int>& lcutters, const std::map<Vertex, int>& rcutters, Tree& fractureTree)
{
    Tree repGraph; // The representative graph, relevant for prime nodes.
    //TODO: I need the Permutation A^-1 for ORDER
    size_t out_d;
    size_t in_d;
    bool foundChild = false;
    
    // F.L.: I use this for order.
    size_t max_degree = fractureTree[v].containedNodes.size();
    std::vector<bool> out_degrees(max_degree,false); // store degrees for ORDER recognition (initialise all degrees with false)
    bool parallel = true;
    bool series = true;
    bool order = true;
    
    svertex_iter si, si_end;
    for (boost::tie(si, si_end) = boost::vertices(tmp); si != si_end; ++si)
    {
        Vertex t = tmp.local_to_global(*si); // we need the global name for searching in the fractureTree
        // TODO: here is the error!
        out_degrees[boost::out_degree(*si,tmp)] = true; // degree sequence
        
        std::vector<Vertex>::iterator child = std::find(fractureTree[v].children.begin(), fractureTree[v].children.end(),t);
        if (child != fractureTree[v].children.end()) // This means, we are a "real" child of the module
        {
            // TODO: here too
            out_d = boost::out_degree(*si,tmp);
            in_d = boost::in_degree(*si, tmp);
            foundChild = true;
            if(out_d != 0 || in_d != 0)
                parallel = false;
            if(out_d != max_degree - 1 || in_d != max_degree -1)
                series = false;
            if(out_d + in_d != max_degree -1)
                order = false;
            if(!parallel && !series && !order)
                break; // we have prime.

        }
        // If we are no real child we will be considered later on (or already have been)
    }


    // TODO: WTF is this??? Happens for 3,4; 4,5 and 6,7. 
    if (!foundChild) // This means we dont have any real children! Assign module type depending on the edges between modules
    {
        // only one child is processed. For order, I might need ALL.
        for (const auto child : boost::adjacent_vertices(v,fractureTree))
        {
            SubGraph g = tmp.create_subgraph();
            for (const auto& c : fractureTree[child].containedNodes)
                boost::add_vertex(c,g);
            //Vertex v1 = g.global_to_local(fractureTree[child].containedNodes[0]); // local degree
            
            size_t numberNodes = max_degree; //fractureTree[v].containedNodes.size();
            size_t numberChildNodes = fractureTree[child].containedNodes.size();

            
            std::cout << "Case not found child. This has " << numberNodes << " nodes, current child has " << numberChildNodes << std::endl;

            for(auto node : fractureTree[child].containedNodes){
            
                Vertex v1 = g.global_to_local(node);
                size_t out_deg_local = boost::out_degree(v1,g);
                size_t out_deg_global = boost::out_degree(v1,tmp);
                // F.L: I need those.
                size_t in_deg_logal = boost::in_degree(v1,g);
                size_t in_deg_global = boost::in_degree(v1, tmp);
                
                if (series && (out_deg_global - out_deg_local != numberNodes - numberChildNodes || in_deg_global - in_deg_logal != numberNodes - numberChildNodes))
                    series = false;
                if(parallel && (out_deg_global - out_deg_local != 0 || in_deg_global - in_deg_logal == 0)){
                    parallel = false;
                    // todo: does this also apply to ORDER?
                    order = false;
                }
                
                out_degrees[out_deg_local] = true;
                // todo: verify & test this.
                if(order && (out_deg_global + in_deg_global != numberChildNodes-1 || out_deg_local + in_deg_logal != numberChildNodes-1))
                    order = false;
                
                
                std::cout << "Out: local - " << out_deg_local << ", global - " << out_deg_global << "; In: local - " << in_deg_logal << ", global - " << in_deg_global << std::endl;
            }
            if(!parallel && !series){
                if(!order){
                    std::cout << "Type: Prime." << std::endl;
                    //break; // might do that here
                } else {
                    std::cout << "Type might be Order." << std::endl;
                }
            }
        }
    }
    
    // assign the module type.
    if (parallel)
        fractureTree[v].type = "Parallel";
    else if (series)
        fractureTree[v].type = "Series";
    else{
        // out degree is neither 0 nor k-1 => prime or order.
        if(order){
            for(bool isOrder : out_degrees){
                if(!isOrder)
                    order = false;
            }
        }
        // todo: notnagel, damit der nicht ewig läuft. Muss natürlich auch merging in order prüfen (und für andere??? :( )
        if(order){
            fractureTree[v].type = "Order";
        } else {
            fractureTree[v].type = "Prime";
            detectMerging(tmp.root(), graph, v, fac, minPos, maxPos, lcutters, rcutters, fractureTree);
            // this also assigns the module type if still missing (this is a lie)
        }
    }
    // TODO: detect merging for ORDER! For SERIES/PARALLEL not necessary?
    
    // In directed graphs, weak ORDER modules can exist...
    // TODO: detect here or in my code!
}

// assigns the type to an internal module
void assignModuleType(SubGraph& rootG, const Graph& graph, TVertex v, const std::vector<Vertex>& fac, const std::map<Vertex, int>& lcutters, const std::map<Vertex, int>& rcutters, Tree& fractureTree)
{
    const auto& facPos = boost::get(facPos_t(), graph);
    SubGraph tmp = rootG.create_subgraph();
    int minPos = boost::num_vertices(rootG); //for module merging if prime
    int maxPos = 0;
    int ct = 0;
    for (const auto& vert : fractureTree[v].containedNodes){
        ct++;
        boost::add_vertex(vert,tmp);  // They are already part!
        minPos = std::min(minPos, boost::get(facPos,vert));
        maxPos = std::max(maxPos, boost::get(facPos,vert));
    }
    // todo: I can do better, here.
    size_t numEdges = boost::num_edges(tmp);
    size_t numVertices = boost::num_vertices(tmp);
    std::cout << "edges: " << numEdges << ", vertices: " << numVertices << std::endl;
    if (numEdges == 0){ //No edges between children nodes => parallel
        fractureTree[v].type = "Parallel";
        std::cout << " is parallel node." << std::endl;
    }
    else if (numEdges == numVertices * (numVertices - 1)){ // all edges between children => series // F.L. 11.12.: double amount.
        fractureTree[v].type = "Series";
        std::cout << " is series node." << std::endl;
    }
    else // Then its more complex: Order or Prime
        calculateModuleType(graph, tmp, v, fac, minPos, maxPos, lcutters, rcutters, fractureTree);
}

// Finally builds the modular decomposition with the reduced fracture tree and the graph structure
void ModDecomp::buildModDecomp(const Graph& graph, const std::vector<Vertex>& fac, const std::map<Vertex, int>& lcutters, const std::map<Vertex, int>& rcutters, Tree& fractureTree) //we need representative graphs for module merging, factorization for module merging
{
    SubGraph rootG;
    boost::copy_graph(graph, rootG);
    
    tvertex_iter ti,ti_end;
    for (boost::tie(ti,ti_end) = boost::vertices(fractureTree); ti != ti_end; ++ti)
    {
        if (fractureTree[*ti].name == "Internal" and fractureTree[*ti].type != "Series" and fractureTree[*ti].type != "Parallel" and fractureTree[*ti].type != "Order" and fractureTree[*ti].type != "Prime")
        { //We are a module - need to assign type (if we are not a merged node whichs type already has been assigned in creation
            std::cout << "Assigning inner module type in last step." << std::endl;
            assignModuleType(rootG, graph, *ti, fac, lcutters, rcutters, fractureTree);
        }
        //printTree(fractureTree);
    }
}

// F.L.: need another check for weak order and dummy primes.
bool ModDecomp::deleteWeakOrderAndDummies(Tree& fractureTree){
    tvertex_iter vi, vi_end, next;
    boost::tie(vi,vi_end) = boost::vertices(fractureTree);
    bool change = false;
    for (next = vi; vi != vi_end; vi = next)
    {
        ++next;
        // Note: I can't use "fractureTree[*vi].toDelete" because it's sometimes true already for whatever reason.
        
        // dummy prime
        bool toDelete = boost::out_degree(*vi, fractureTree) == 1;
       
        // not for root (no parent) and only for order: weak module deletion.
        if(!toDelete && fractureTree[*vi].type == "Order" && !fractureTree[*vi].root){
            TVertex parent = boost::source(*(boost::in_edges(*vi,fractureTree).first),fractureTree);
            toDelete = fractureTree[parent].type == "Order";
        }
        if(toDelete){
            std::cout << "Deleting. type: " << fractureTree[*vi].type << ", root: " << fractureTree[*vi].root << std::endl;
            change = true;
            if (!fractureTree[*vi].root){
                deleteVertex(*vi, fractureTree);
            } else{
                auto child = *boost::adjacent_vertices(*vi, fractureTree).first;
                fractureTree[child].root = true;
                boost::remove_edge(*(boost::out_edges(*vi,fractureTree).first),fractureTree);
                boost::remove_vertex(*vi,fractureTree);
            }
        }
    }
    return change;
}

// Given the factorizing permutation calculates the modular decomposition
Tree ModDecomp::calcModDecomp(const std::vector<Vertex>& factorization, Graph& graph)
{
    const auto& facPos = boost::get(facPos_t(), graph);
    int ct = 0;
    for (const auto& v : factorization)
        boost::put(facPos, v, ct++); // facPos initialized here.
    
    std::map<Vertex, int> lcutters;
    std::map<Vertex, int> rcutters; // rcutter of vertex
    // Check for connected components first
    DyckWord dyck = parenthesizing(factorization, graph, lcutters, rcutters); // cutters get set here
    std::cout << dyck.toString(graph) << std::endl;
    
    Tree fractureTree = buildFractureTree(dyck, graph, lcutters, rcutters);
    std::cout << "Fracture Tree: " << std::endl;
    //printTree(fractureTree,true);
    
    moduleDetDel(fractureTree); // ok!
       printTree(fractureTree);
    buildModDecomp(graph, factorization, lcutters, rcutters, fractureTree); // ok, if calculateModuleType is ok.
    //   printTree(fractureTree);
    // F.L. Final scan
    //std::cout << "Before deletion:" << std::endl;
    //printTree(fractureTree);
    deleteWeakOrderAndDummies(fractureTree);
    
    
    return fractureTree;
}




// F.L. 19.11.17: added
Graph ModDecomp::readFromString(std::string inputGraph, std::vector<Vertex>& facPerm, std::string factPermStr){
    
    char openDelim = '(';
    char closeDelim = ')';
    size_t splitIndex = inputGraph.find_first_of(']');
    Graph g;
    
    typedef boost::split_iterator<std::string::iterator> string_split_iterator;
    std::unordered_map<std::string, Vertex> nameToVertex;
    const auto& name = boost::get(boost::vertex_name, g);
    
    // read vertices
    std::string vertexLine = inputGraph.substr(2,splitIndex-2);
    for(string_split_iterator It= make_split_iterator(vertexLine, first_finder(", ", boost::is_iequal()));
        It!=string_split_iterator();++It){
        std::string vname = boost::copy_range<std::string>(*It);
        //std::cout << vname << std::endl;
        Vertex v = boost::add_vertex(g);
        nameToVertex.emplace(vname,v);
        boost::put(name, v, vname);
    }
    
    // read fact perm
    for(string_split_iterator It= make_split_iterator(factPermStr, first_finder(", ", boost::is_iequal()));
        It!=string_split_iterator();++It){
        facPerm.push_back( nameToVertex[boost::copy_range<std::string>(*It)] );
    }
    
    std::string edgeLine = inputGraph.substr(splitIndex + 4, inputGraph.length() -splitIndex -6);
    //std::cout << vertexLine << "\n" << edgeLine << "\n"<< splitIndex << std::endl;
    
    // read edges
    unsigned edgeCount = 0;
    for(string_split_iterator It= make_split_iterator(edgeLine, first_finder(", ", boost::is_iequal()));
        It!=string_split_iterator();++It){
        std::string current = boost::copy_range<std::string>(*It);
        if(current.front() == openDelim && current.back() == closeDelim){
            size_t edgeSplit = current.find_first_of(',');
            Vertex v1 = nameToVertex[ current.substr(1,edgeSplit-1) ];
            Vertex v2 = nameToVertex[ current.substr(edgeSplit+1, current.length() -edgeSplit -2) ];
            boost::add_edge(v1,v2,0,g);
            edgeCount++;
            //std::cout << "v1: " << v1 << " v2: " << v2 << std::endl;
        } else {
            std::cerr << "invalid edge: " << current << std::endl;
        }
        //std::cout << current << std::endl;
    }
    
    return g;
}



int main(int argc, char* argv[])
{
    ModDecomp md;
    std::vector<Vertex> facPerm;
    
    
    if(argc == 1){
        // no further arguments: just do the example code
        // from the paper
        //std::string test("([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13], [(1,4), (1,5), (1,3), (2,1), (3,2), (3,5), (3,4), (4,2), (5,4), (5,2), (6,7), (6,8), (6,9), (6,10), (7,8), (7,9), (7,10), (8,9), (8,10), (10,12), (10,13), (10,11), (10,0), (11,0), (11,9), (11,10), (12,9), (12,11), (12,13), (12,10), (13,9), (13,11), (13,10), (13,12), (0,9), (0,11), (0,10)])");
        // from my code: "1, 3, 5, 4, 2, 10, 9, 6, 7, 8, 13, 12, 11, 0";
        //std::string fpstr = "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 0";
         
        /*
        std::string test("([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22], [(0,5), (0,6), (0,7), (0,8), (0,9), (0,10), (0,11), (0,13), (0,14), (0,15), (1,5), (1,6), (1,7), (1,8), (1,9), (1,10), (1,11), (1,13), (1,14), (1,15), (2,3), (2,4), (2,5), (2,6), (2,7), (2,8), (2,9), (2,10), (2,11), (2,13), (2,14), (2,15), (2,19), (2,20), (2,21), (2,22), (3,2), (3,4), (3,5), (3,6), (3,7), (3,8), (3,10), (3,11), (3,13), (3,14), (3,15), (3,19), (3,20), (3,21), (3,22), (4,2), (4,3), (4,5), (4,6), (4,7), (4,8), (4,9), (4,10), (4,11), (4,13), (4,14), (4,15), (4,19), (4,20), (4,21), (4,22), (5,0), (5,1), (5,2), (5,3), (5,4), (5,7), (5,8), (5,9), (5,10), (5,11), (5,12), (5,13), (5,14), (5,15), (5,16), (5,17), (5,18), (5,19), (5,20), (5,21), (5,22), (6,0), (6,1), (6,2), (6,3), (6,4), (6,7), (6,8), (6,9), (6,10), (6,11), (6,12), (6,13), (6,14), (6,15), (6,16), (6,17), (6,18), (6,19), (6,20), (6,21), (6,22), (7,0), (7,1), (7,2), (7,3), (7,4), (7,5), (7,6), (7,8), (7,9), (7,10), (7,11), (7,12), (7,13), (7,14), (7,15), (7,16), (7,17), (7,18), (7,19), (7,20), (7,21), (7,22), (8,0), (8,1), (8,2), (8,3), (8,4), (8,5), (8,6), (8,7), (8,10), (8,11), (8,12), (8,13), (8,14), (8,15), (8,16), (8,17), (8,18), (8,19), (8,20), (8,21), (8,22), (9,0), (9,1), (9,2), (9,3), (9,4), (9,5), (9,6), (9,7), (9,8), (9,10), (9,11), (9,12), (9,13), (9,14), (9,15), (9,16), (9,17), (9,18), (9,19), (9,20), (9,21), (9,22), (10,0), (10,1), (10,2), (10,3), (10,4), (10,5), (10,6), (10,7), (10,8), (10,9), (10,12), (10,16), (10,17), (10,18), (10,19), (10,20), (10,21), (10,22), (11,0), (11,1), (11,2), (11,3), (11,4), (11,5), (11,6), (11,7), (11,8), (11,9), (11,12), (11,16), (11,17), (11,18), (11,19), (11,20), (11,21), (11,22), (12,5), (12,6), (12,7), (12,8), (12,9), (12,10), (12,11), (12,13), (12,14), (12,15), (13,0), (13,1), (13,2), (13,3), (13,4), (13,5), (13,6), (13,7), (13,8), (13,9), (13,11), (13,12), (13,16), (13,17), (13,18), (13,19), (13,20), (13,21), (13,22), (14,0), (14,1), (14,2), (14,3), (14,4), (14,5), (14,6), (14,7), (14,8), (14,9), (14,12), (14,15), (14,16), (14,17), (14,18), (14,19), (14,20), (14,21), (14,22), (15,0), (15,1), (15,2), (15,3), (15,4), (15,5), (15,6), (15,7), (15,8), (15,9), (15,12), (15,14), (15,16), (15,17), (15,18), (15,19), (15,20), (15,21), (15,22), (16,5), (16,6), (16,7), (16,8), (16,9), (16,10), (16,11), (16,13), (16,14), (16,15), (16,20), (17,3), (17,5), (17,6), (17,7), (17,8), (17,9), (17,10), (17,11), (17,13), (17,14), (17,15), (18,5), (18,6), (18,7), (18,8), (18,9), (18,10), (18,11), (18,13), (18,14), (18,15), (19,2), (19,3), (19,4), (19,5), (19,6), (19,7), (19,8), (19,9), (19,10), (19,11), (19,13), (19,14), (19,15), (19,18), (19,20), (19,21), (19,22), (20,2), (20,4), (20,5), (20,6), (20,7), (20,8), (20,9), (20,10), (20,11), (20,13), (20,14), (20,19), (20,21), (20,22), (21,2), (21,4), (21,5), (21,6), (21,7), (21,8), (21,9), (21,11), (21,13), (21,14), (21,15), (21,19), (21,20), (21,22), (22,2), (22,3), (22,4), (22,5), (22,6), (22,7), (22,8), (22,9), (22,10), (22,13), (22,14), (22,15), (22,19), (22,20), (22,21)])");
        std::string fpstr = "7, 6, 5, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 11, 10, 9, 8, 3, 4, 2, 12, 1, 0";
         */
        
        // Error: testGraphs/DMDvery/randDigraph_n_11_edits_4_11-30_15:34:03:245_original.txt
        std::string test("([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10], [(0,2), (0,5), (0,6), (0,7), (0,8), (0,9), (0,10), (1,2), (1,5), (1,6), (1,7), (1,8), (1,10), (2,0), (2,1), (2,3), (2,4), (2,5), (2,6), (2,7), (2,8), (2,10), (3,2), (3,5), (3,6), (3,7), (3,8), (3,9), (3,10), (4,2), (4,5), (4,6), (4,7), (4,8), (4,9), (4,10), (5,0), (5,1), (5,2), (5,3), (5,4), (5,7), (5,8), (5,9), (5,10), (6,0), (6,1), (6,2), (6,3), (6,4), (6,7), (6,8), (6,9), (6,10), (7,0), (7,1), (7,2), (7,3), (7,4), (7,5), (7,6), (7,9), (7,10), (8,0), (8,1), (8,2), (8,3), (8,4), (8,5), (8,6), (8,9), (8,10), (9,0), (9,1), (9,2), (9,3), (9,4), (9,5), (9,6), (9,8), (9,10), (10,0), (10,1), (10,2), (10,3), (10,5), (10,6), (10,7), (10,8)])");
                         
        std::string fpstr = "6, 5, 10, 9, 8, 7, 4, 2, 1, 3, 0";
        
        Graph g = md.readFromString(test, facPerm, fpstr);
        std::cout << "Input Graph:" << std::endl;
        printGraph(g);
        
        Tree md_tree = md.calcModDecomp(facPerm, g);
        std::cout << "MD Tree:" << std::endl;
        printTree(md_tree);
        
    }
    
    else if( argc == 3){
        Graph g = md.readFromString(argv[1],facPerm, argv[2]);
        Tree t = md.calcModDecomp(facPerm, g);
        std::cout << "MD Tree:" << std::endl;
        printTree(t);
    }
    
}

