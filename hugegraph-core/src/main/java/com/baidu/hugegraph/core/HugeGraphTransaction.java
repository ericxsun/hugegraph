// Copyright 2017 HugeGraph Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.baidu.hugegraph.core;

import com.baidu.hugegraph.graphdb.relations.RelationIdentifier;

/**
 * HugeGraphTransaction defines a transactional context for a {@link HugeGraph}. Since HugeGraph is a transactional graph
 * database, all interactions with the graph are mitigated by a HugeGraphTransaction.
 * <p/>
 * All vertex and edge retrievals are channeled by a graph transaction which bundles all such retrievals, creations and
 * deletions into one transaction. A graph transaction is analogous to a
 * <a href="http://en.wikipedia.org/wiki/Database_transaction">database transaction</a>.
 * The isolation level and <a href="http://en.wikipedia.org/wiki/ACID">ACID support</a> are configured through the storage
 * backend, meaning whatever level of isolation is supported by the storage backend is mirrored by a graph transaction.
 * <p/>
 * A graph transaction supports:
 * <ul>
 * <li>Creating vertices, properties and edges</li>
 * <li>Creating types</li>
 * <li>Index-based retrieval of vertices</li>
 * <li>Querying edges and vertices</li>
 * <li>Aborting and committing transaction</li>
 * </ul>
 *
 * @author Matthias Br&ouml;cheler (http://www.matthiasb.com)
 */
public interface HugeGraphTransaction extends Transaction {

   /* ---------------------------------------------------------------
    * Modifications
    * ---------------------------------------------------------------
    */

    /**
     * Creates a new vertex in the graph with the given vertex id and the given vertex label.
     * Note, that an exception is thrown if the vertex id is not a valid HugeGraph vertex id or if a vertex with the given
     * id already exists.
     * <p/>
     * Custom id setting must be enabled via the configuration option {@link com.baidu.hugegraph.graphdb.configuration.GraphDatabaseConfiguration#ALLOW_SETTING_VERTEX_ID}.
     * <p/>
     * Use {@link com.baidu.hugegraph.core.util.HugeGraphId#toVertexId(long)} to construct a valid HugeGraph vertex id from a user id.
     *
     * @param id vertex id of the vertex to be created
     * @param vertexLabel vertex label for this vertex - can be null if no vertex label should be set.
     * @return New vertex
     */
    public HugeGraphVertex addVertex(Long id, VertexLabel vertexLabel);

    /**
     * Retrieves the vertex for the specified id.
     *
     * This method is intended for internal use only. Use {@link org.apache.tinkerpop.gremlin.structure.Graph#vertices(Object...)} instead.
     *
     * @param id id of the vertex to retrieve
     * @return vertex with the given id if it exists, else null
     */
    public HugeGraphVertex getVertex(long id);


    public Iterable<HugeGraphVertex> getVertices(long... ids);

    public Iterable<HugeGraphEdge> getEdges(RelationIdentifier... ids);

   /* ---------------------------------------------------------------
    * Closing and admin
    * ---------------------------------------------------------------
    */

    /**
     * Commits and closes the transaction.
     * <p/>
     * Will attempt to persist all modifications which may result in exceptions in case of persistence failures or
     * lock contention.
     * <br />
     * The call releases data structures if possible. All element references (e.g. vertex objects) retrieved
     * through this transaction are stale after the transaction closes and should no longer be used.
     *
     * @throws com.baidu.hugegraph.diskstorage.BackendException
     *          if an error arises during persistence
     */
    public void commit();

    /**
     * Aborts and closes the transaction. Will discard all modifications.
     * <p/>
     * The call releases data structures if possible. All element references (e.g. vertex objects) retrieved
     * through this transaction are stale after the transaction closes and should no longer be used.
     *
     * @throws com.baidu.hugegraph.diskstorage.BackendException
     *          if an error arises when releasing the transaction handle
     */
    public void rollback();

    /**
     * Checks whether the transaction is still open.
     *
     * @return true, when the transaction is open, else false
     */
    public boolean isOpen();

    /**
     * Checks whether the transaction has been closed.
     *
     * @return true, if the transaction has been closed, else false
     */
    public boolean isClosed();

    /**
     * Checks whether any changes to the graph database have been made in this transaction.
     * <p/>
     * A modification may be an edge or vertex update, addition, or deletion.
     *
     * @return true, if the transaction contains updates, else false.
     */
    public boolean hasModifications();

}