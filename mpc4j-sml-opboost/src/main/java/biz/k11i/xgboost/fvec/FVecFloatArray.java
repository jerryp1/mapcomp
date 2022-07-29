/*
 * Original Work Copyright 2018 H2O.ai.
 * Modified Work Copyright 2021 Weiran Liu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package biz.k11i.xgboost.fvec;

/**
 * Feature Vector Float Array.
 *
 * @author KOMIYA Atsushi, Michal Kurka, Weiran Liu
 * @date 2021/10/08
 */
class FVecFloatArray implements FVec {
    private static final long serialVersionUID = -6252541085408935802L;
    /**
     * float value array
     */
    private final float[] values;
    /**
     * whether treat 0 as N/A
     */
    private final boolean treatsZeroAsNA;

    FVecFloatArray(float[] values, boolean treatsZeroAsNA) {
        this.values = values;
        this.treatsZeroAsNA = treatsZeroAsNA;
    }

    @Override
    public float featureValue(int index) {
        if (values.length <= index) {
            return Float.NaN;
        }

        float result = values[index];
        if (treatsZeroAsNA && result == 0) {
            return Float.NaN;
        }

        return result;
    }
}
