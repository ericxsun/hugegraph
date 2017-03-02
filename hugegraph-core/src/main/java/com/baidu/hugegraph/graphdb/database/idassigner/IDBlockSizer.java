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

package com.baidu.hugegraph.graphdb.database.idassigner;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 */

public interface IDBlockSizer {

    /**
     * The size of the id block to be returned by calls {@link com.baidu.hugegraph.diskstorage.IDAuthority#getIDBlock(int,int, Duration)}
     * for the given id namespace.
     * In other words, for the returned array of the above mentioned call, it must hold that the difference between the second
     * and first value is equal to the block size returned by this method (for the same partition id).
     *
     * @param idNamespace
     * @return
     */
    public long getBlockSize(int idNamespace);

    /**
     * Returns the upper bound for any id block returned by {@link com.baidu.hugegraph.diskstorage.IDAuthority#getIDBlock(int,int, Duration)}
     * for the given id namespace.
     * In other words, it must hold that the second value of the returned array is smaller than this value for the same partition id.
     *
     * @param idNamespace
     * @return
     */
    public long getIdUpperBound(int idNamespace);

}