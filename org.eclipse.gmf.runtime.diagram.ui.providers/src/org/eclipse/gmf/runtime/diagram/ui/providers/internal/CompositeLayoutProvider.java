/******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package org.eclipse.gmf.runtime.diagram.ui.providers.internal;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.CompoundDirectedGraph;
import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.DirectedGraphLayout;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.draw2d.graph.NodeList;
import org.eclipse.draw2d.graph.Subgraph;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gmf.runtime.diagram.ui.editparts.GraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IBorderItemEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.ShapeCompartmentEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.ShapeEditPart;
import org.eclipse.gmf.runtime.draw2d.ui.internal.graph.CompositeDirectedGraphLayout;
import org.eclipse.gmf.runtime.draw2d.ui.internal.graph.VirtualNode;

/**
 * Provider that creates a command for the CompoundDirectedGraph layout in GEF.
 * 
 * @author mmostafa
 * @canBeSeenBy org.eclipse.gmf.runtime.diagram.ui.providers.*
 * 
 */

public abstract class CompositeLayoutProvider
    extends DefaultProvider {
    
    /* (non-Javadoc)
     * @see org.eclipse.gmf.runtime.diagram.ui.providers.internal.DefaultProvider#build_nodes(java.util.List, java.util.Map, org.eclipse.draw2d.graph.Subgraph)
     */
    protected NodeList build_nodes(List selectedObjects,
            Map editPartToNodeDict, Subgraph rootGraph) {
        ListIterator li = selectedObjects.listIterator();
        NodeList nodes = new NodeList();
        while (li.hasNext()) {
            IGraphicalEditPart gep = (IGraphicalEditPart) li.next();
            boolean hasChildren = hasChildren(gep);
            if (!(gep instanceof IBorderItemEditPart)
                && (gep instanceof ShapeEditPart || gep instanceof ShapeCompartmentEditPart)) {
                GraphicalEditPart ep = (GraphicalEditPart) gep;
                Point position = ep.getFigure().getBounds().getLocation();
                if (minX == -1) {
                    minX = position.x;
                    minY = position.y;
                } else {
                    minX = Math.min(minX, position.x);
                    minY = Math.min(minY, position.y);
                }
                Node n = null;
                if (hasChildren) {
                    if (rootGraph != null)
                        n = new Subgraph(ep, rootGraph);
                    else
                        n = new Subgraph(ep);
                } else {
                    if (rootGraph != null)
                        n = new Node(ep, rootGraph);
                    else
                        n = new Node(ep);
                }
                adjustNodePadding(n, editPartToNodeDict);
                Dimension size = ep.getFigure().getBounds().getSize();
                setNodeMetrics(n, new Rectangle(position.x, position.y,
                    size.width, size.height));
                editPartToNodeDict.put(ep, n);
                nodes.add(n);
                if (hasChildren) {
                    build_nodes(gep.getChildren(), editPartToNodeDict,
                        (Subgraph) n);
                }
            }
        }
        return nodes;
    }

    /* (non-Javadoc)
     * @see org.eclipse.gmf.runtime.diagram.ui.providers.internal.DefaultProvider#createGraphLayout()
     */
    protected DirectedGraphLayout createGraphLayout() {
        return new CompositeDirectedGraphLayout();
    }

    /* (non-Javadoc)
     * @see org.eclipse.gmf.runtime.diagram.ui.providers.internal.DefaultProvider#createNodeChangeBoundCommands(org.eclipse.draw2d.graph.DirectedGraph, org.eclipse.draw2d.geometry.Point)
     */
    protected Command createNodeChangeBoundCommands(DirectedGraph g, Point diff) {
        CompoundCommand cc = new CompoundCommand(""); //$NON-NLS-1$
        NodeList list = new NodeList();
        NodeList subGraphs = ((CompoundDirectedGraph) g).nodes;
        list.addAll(subGraphs);
        for (Iterator iter = subGraphs.iterator(); iter.hasNext();) {
            Node element = (Node) iter.next();
            if (element instanceof Subgraph)
                list.addAll(getAllMembers((Subgraph) element));
        }
        createSubCommands(diff, list.listIterator(), cc);
        if (cc.isEmpty())
            return null;
        return cc;
    }

    private Collection getAllMembers(Subgraph element) {
        NodeList list = new NodeList();
        list.addAll(element.members);
        for (Iterator iter = element.members.iterator(); iter.hasNext();) {
            Node node = (Node) iter.next();
            if (node instanceof Subgraph)
                list.addAll(getAllMembers((Subgraph) node));
        }
        return list;
    }

    /* (non-Javadoc)
     * @see org.eclipse.gmf.runtime.diagram.ui.providers.internal.DefaultProvider#getNodeMetrics(org.eclipse.draw2d.graph.Node)
     */
    protected Rectangle getNodeMetrics(Node n) {
        Rectangle rect = null;
        if (n.getParent() instanceof VirtualNode) {
            Node parent = n.getParent();
            rect = new Rectangle(n.x + parent.x, n.y + parent.y, n.width,
                n.height);
        } else
            rect = new Rectangle(n.x, n.y, n.width, n.height);
        return translateFromGraph(rect);
    }
    
    protected void postProcessGraph(DirectedGraph g, Hashtable editPartToNodeDict) {
        //default do nothing
    }
    
    /**
     * @param gep
     * @return
     */
    protected boolean hasChildren(IGraphicalEditPart gep) {
        List children = gep.getChildren();
        boolean hasChildren = false;
        if (!children.isEmpty()){
            for (Iterator iter = children.iterator(); iter.hasNext() && !hasChildren;) {
                Object element = iter.next();
                if (!(element instanceof IBorderItemEditPart) &&
                        ( element instanceof ShapeEditPart ||
                          element instanceof ShapeCompartmentEditPart)){
                    hasChildren = true;
                }else
                    hasChildren = hasChildren((IGraphicalEditPart)element);
            }
        }
        return hasChildren;
    }
    
    /**
     * this method will adjust the passed node Padding; the default implementatio 
     * will use a fixed Padding then it will consider adding extra Padding if the 
     * node parent is not a direct parent
     * clients can override this method to change the behaviour
     * @param node the node to adust the padding for
     */
    protected void adjustNodePadding(Node node,Map editPartToNodeDict) {
        Insets padding  = new Insets(NODE_PADDING);
        GraphicalEditPart ep = (GraphicalEditPart)node.data;
        // check if the direct parent is added already to the graph
        GraphicalEditPart parent = (GraphicalEditPart)ep.getParent();
        if (parent != null &&
            node.getParent() != null &&
            editPartToNodeDict.get(parent)!=node.getParent()){
            // now the direct parent is not added to the graph so, we had 
            // to adjust the padding of the node to consider the parent
            IFigure thisFigure = parent.getFigure();
            IFigure parentFigure = ((GraphicalEditPart)node.getParent().data).getFigure();
            Point parentLocation = parentFigure.getBounds().getLocation();
            Point nodeLocation = thisFigure.getBounds().getLocation();
            thisFigure.translateToAbsolute(nodeLocation);
            parentFigure.translateToAbsolute(parentLocation);
            Dimension delta = nodeLocation.getDifference(parentLocation);
            Rectangle rect = translateToGraph(new Rectangle(delta.width , delta.height , 0 , 0));
            padding.top  += rect.y ;
            padding.left += rect.x;
        }
        node.setPadding(padding);
    }

    
    /* (non-Javadoc)
     * @see org.eclipse.gmf.runtime.diagram.ui.providers.internal.DefaultProvider#createGraph()
     */
    protected DirectedGraph createGraph(){
        return new CompoundDirectedGraph();
    }

   
}