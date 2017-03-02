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

package com.baidu.hugegraph.graphdb.query.graph;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.baidu.hugegraph.core.*;
import com.baidu.hugegraph.core.schema.Parameter;
import com.baidu.hugegraph.diskstorage.indexing.RawQuery;
import com.baidu.hugegraph.graphdb.database.IndexSerializer;
import com.baidu.hugegraph.graphdb.internal.ElementCategory;
import com.baidu.hugegraph.graphdb.query.BaseQuery;
import com.baidu.hugegraph.graphdb.transaction.StandardHugeGraphTx;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Implementation of {@link HugeGraphIndexQuery} for string based queries that are issued directly against the specified
 * indexing backend. It is assumed that the given string conforms to the query language of the indexing backend.
 * This class does not understand or verify the provided query. However, it will introspect the query and replace
 * any reference to `v.SOME_KEY`, `e.SOME_KEY` or `p.SOME_KEY` with the respective key reference. This replacement
 * is 'dumb' in the sense that it relies on simple string replacements to accomplish this. If the key contains special characters
 * (in particular space) then it must be encapsulated in quotation marks.
 * </p>
 * In addition to the query string, a number of parameters can be specified which will be passed verbatim to the indexing
 * backend during query execution.
 * </p>
 * This class essentially just acts as a builder, uses the {@link IndexSerializer} to execute the query, and then post-processes
 * the result set to return to the user.
 *
 * @author Matthias Broecheler (me@matthiasb.com)
 */
public class IndexQueryBuilder extends BaseQuery implements HugeGraphIndexQuery {

    private static final Logger log = LoggerFactory.getLogger(IndexQueryBuilder.class);


    private static final String VERTEX_PREFIX = "v.";
    private static final String EDGE_PREFIX = "e.";
    private static final String PROPERTY_PREFIX = "p.";

    private final StandardHugeGraphTx tx;
    private final IndexSerializer serializer;

    /**
     * The name of the indexing backend this query is directed at
     */
    private String indexName;
    /**
     * Query string conforming to the query language supported by the indexing backend.
     */
    private String query;
    /**
     * Parameters passed to the indexing backend during query execution to modify the execution behavior.
     */
    private final List<Parameter> parameters;

    /**
     * Prefix to be used to identify vertex, edge or property references and trigger key parsing and conversion.
     * In most cases this will be one of the above defined static prefixes, but in some special cases, the user may
     * define this.
     */
    private String prefix;
    /**
     * Name to use for unknown keys, i.e. key references that could not be resolved to an actual type in the database.
     */
    private final String unkownKeyName;
    /**
     * In addition to limit, this type of query supports offsets.
     */
    private int offset;


    public IndexQueryBuilder(StandardHugeGraphTx tx, IndexSerializer serializer) {
        Preconditions.checkNotNull(tx);
        Preconditions.checkNotNull(serializer);
        this.tx = tx;
        this.serializer = serializer;

        parameters = Lists.newArrayList();
        unkownKeyName = tx.getGraph().getConfiguration().getUnknownIndexKeyName();
        this.offset=0;
    }

    //################################################
    // Inspection Methods
    //################################################

    public String getIndex() {
        return indexName;
    }

    public Parameter[] getParameters() {
        return parameters.toArray(new Parameter[parameters.size()]);
    }

    public String getQuery() {
        return query;
    }

    public int getOffset() {
        return offset;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public IndexQueryBuilder setElementIdentifier(String identifier) {
        Preconditions.checkArgument(StringUtils.isNotBlank(identifier),"Prefix may not be a blank string");
        this.prefix=identifier;
        return this;
    }

    public String getUnknownKeyName() {
        return unkownKeyName;
    }


    //################################################
    // Builder Methods
    //################################################

    public IndexQueryBuilder setIndex(String indexName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(indexName));
        this.indexName=indexName;
        return this;
    }

    public IndexQueryBuilder setQuery(String query) {
        Preconditions.checkArgument(StringUtils.isNotBlank(query));
        this.query=query;
        return this;
    }

    @Override
    public IndexQueryBuilder offset(int offset) {
        Preconditions.checkArgument(offset>=0,"Invalid offset provided: %s",offset);
        this.offset=offset;
        return this;
    }

    @Override
    public IndexQueryBuilder limit(int limit) {
        super.setLimit(limit);
        return this;
    }

    @Override
    public IndexQueryBuilder addParameter(Parameter para) {
        parameters.add(para);
        return this;
    }

    @Override
    public IndexQueryBuilder addParameters(Iterable<Parameter> paras) {
        Iterables.addAll(parameters, paras);
        return this;
    }

    @Override
    public IndexQueryBuilder addParameters(Parameter... paras) {
        for (Parameter para: paras) addParameter(para);
        return this;
    }

    private Iterable<Result<HugeGraphElement>> execute(ElementCategory resultType) {
        Preconditions.checkNotNull(indexName);
        Preconditions.checkNotNull(query);
        if (tx.hasModifications())
            log.warn("Modifications in this transaction might not be accurately reflected in this index query: {}",query);
        Iterable<RawQuery.Result> result = serializer.executeQuery(this,resultType,tx.getTxHandle(),tx);
        final Function<Object, ? extends HugeGraphElement> conversionFct = tx.getConversionFunction(resultType);
        return Iterables.filter(Iterables.transform(result, new Function<RawQuery.Result, Result<HugeGraphElement>>() {
            @Nullable
            @Override
            public Result<HugeGraphElement> apply(@Nullable RawQuery.Result result) {
                return new ResultImpl<HugeGraphElement>(conversionFct.apply(result.getResult()),result.getScore());
            }
        }),new Predicate<Result<HugeGraphElement>>() {
            @Override
            public boolean apply(@Nullable Result<HugeGraphElement> r) {
                return !r.getElement().isRemoved();
            }
        });
    }

    @Override
    public Iterable<Result<HugeGraphVertex>> vertices() {
        setPrefixInternal(VERTEX_PREFIX);
        return (Iterable)execute(ElementCategory.VERTEX);
    }

    @Override
    public Iterable<Result<HugeGraphEdge>> edges() {
        setPrefixInternal(EDGE_PREFIX);
        return (Iterable)execute(ElementCategory.EDGE);
    }

    @Override
    public Iterable<Result<HugeGraphVertexProperty>> properties() {
        setPrefixInternal(PROPERTY_PREFIX);
        return (Iterable)execute(ElementCategory.PROPERTY);
    }

    private void setPrefixInternal(String prefix) {
        Preconditions.checkArgument(StringUtils.isNotBlank(prefix));
        if (this.prefix==null) this.prefix=prefix;
    }

    private static class ResultImpl<V extends Element> implements Result<V> {

        private final V element;
        private final double score;

        private ResultImpl(V element, double score) {
            this.element = element;
            this.score = score;
        }

        @Override
        public V getElement() {
            return element;
        }

        @Override
        public double getScore() {
            return score;
        }
    }
}