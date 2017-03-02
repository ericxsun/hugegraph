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

package com.baidu.hugegraph.diskstorage.keycolumnvalue.keyvalue;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.baidu.hugegraph.diskstorage.StaticBuffer;

/**
 * A {@link KeySelector} utility that can be generated out of a given {@link KVQuery}
 *
 * @author Matthias Br&ouml;cheler (me@matthiasb.com);
 */
public class KeySelector {

    private final Predicate<StaticBuffer> keyFilter;
    private final int limit;
    private int count;

    public KeySelector(Predicate<StaticBuffer> keyFilter, int limit) {
        Preconditions.checkArgument(limit > 0, "The count limit needs to be positive. Given: " + limit);
        Preconditions.checkArgument(keyFilter!=null);
        this.keyFilter = keyFilter;
        this.limit = limit;
        count = 0;
    }

    public static final KeySelector of(int limit) {
        return new KeySelector(Predicates.<StaticBuffer>alwaysTrue(), limit);
    }

    public boolean include(StaticBuffer key) {
        if (keyFilter.apply(key)) {
            count++;
            return true;
        } else return false;
    }

    public boolean reachedLimit() {
        if (count >= limit) return true;
        else return false;
    }

}