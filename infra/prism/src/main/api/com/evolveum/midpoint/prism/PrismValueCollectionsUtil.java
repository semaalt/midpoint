/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 */
public class PrismValueCollectionsUtil {

	public static <T> Collection<T> getValues(Collection<PrismPropertyValue<T>> pvals) {
    	Collection<T> realValues = new ArrayList<>(pvals.size());
		for (PrismPropertyValue<T> pval: pvals) {
			realValues.add(pval.getValue());
		}
		return realValues;
    }

	public static boolean containsRealValue(Collection<PrismPropertyValue<?>> collection, PrismPropertyValue<?> value) {
		for (PrismPropertyValue<?> colVal: collection) {
			if (value.equalsRealValue(colVal)) {
				return true;
			}
		}
		return false;
	}

	public static boolean containsValue(Collection<PrismPropertyValue> collection, PrismPropertyValue value,
			Comparator comparator) {
		for (PrismPropertyValue<?> colVal: collection) {
			if (comparator.compare(colVal, value) == 0) {
				return true;
			}
		}
		return false;
	}

	public static <T> Collection<PrismPropertyValue<T>> createCollection(PrismContext prismContext,
			Collection<T> realValueCollection) {
		Collection<PrismPropertyValue<T>> pvalCol = new ArrayList<>(realValueCollection.size());
		for (T realValue: realValueCollection) {
			pvalCol.add(prismContext.itemFactory().createPrismPropertyValue(realValue));
		}
		return pvalCol;
	}

	public static <T> Collection<PrismPropertyValue<T>> createCollection(PrismContext prismContext, T[] realValueArray) {
		Collection<PrismPropertyValue<T>> pvalCol = new ArrayList<>(realValueArray.length);
		for (T realValue: realValueArray) {
			pvalCol.add(prismContext.itemFactory().createPrismPropertyValue(realValue));
		}
		return pvalCol;
	}

	public static <T> Collection<PrismPropertyValue<T>> wrap(PrismContext prismContext, @NotNull Collection<T> realValues) {
		return realValues.stream()
				.map(val -> prismContext.itemFactory().createPrismPropertyValue(val))
				.collect(Collectors.toList());
	}

	@SafeVarargs
	public static <T> PrismPropertyValue<T>[] wrap(PrismContext prismContext, T... realValues) {
		//noinspection unchecked
		return Arrays.stream(realValues)
				.map(val -> prismContext.itemFactory().createPrismPropertyValue(val))
				.toArray(PrismPropertyValue[]::new);
	}


	@NotNull
	public static List<Referencable> asReferencables(@NotNull Collection<PrismReferenceValue> values) {
		return values.stream().map(prv -> prv.asReferencable()).collect(Collectors.toList());
	}

	@NotNull
	public static List<PrismReferenceValue> asReferenceValues(@NotNull Collection<? extends Referencable> referencables) {
		return referencables.stream().map(ref -> ref.asReferenceValue()).collect(Collectors.toList());
	}

	public static boolean containsOid(Collection<PrismReferenceValue> values, @NotNull String oid) {
		return values.stream().anyMatch(v -> oid.equals(v.getOid()));
	}

	public static <T> void clearParent(List<PrismPropertyValue<T>> values) {
		if (values == null) {
			return;
		}
		for (PrismPropertyValue<T> val: values) {
			val.clearParent();
		}
	}

	public static <V extends PrismValue> boolean containsRealValue(Collection<V> collection, V value) {
		return containsRealValue(collection, value, Function.identity());
	}


	public static <V extends PrismValue> boolean equalsRealValues(Collection<V> collection1, Collection<V> collection2) {
		return MiscUtil.unorderedCollectionEquals(collection1, collection2, (v1, v2) -> v1.equalsRealValue(v2));
	}

	public static <V extends PrismValue> boolean containsAll(Collection<V> thisSet, Collection<V> otherSet, boolean ignoreMetadata, boolean isLiteral) {
		if (thisSet == null && otherSet == null) {
			return true;
		}
		if (otherSet == null) {
			return true;
		}
		if (thisSet == null) {
			return false;
		}
		for (V otherValue: otherSet) {
			if (!contains(thisSet, otherValue, ignoreMetadata, isLiteral)) {
				return false;
			}
		}
		return true;
	}

	@NotNull
	public static <T extends PrismValue> Collection<T> cloneCollection(Collection<T> values) {
		return cloneCollectionComplex(CloneStrategy.LITERAL, values);
	}

	/**
     * Sets all parents to null. This is good if the items are to be "transplanted" into a
     * different Containerable.
     */
	public static <T extends PrismValue> Collection<T> resetParentCollection(Collection<T> values) {
    	for (T value: values) {
    		value.setParent(null);
    	}
    	return values;
	}

	public static <T> Set<T> getRealValuesOfCollection(Collection<? extends PrismValue> collection) {
	    Set<T> retval = new HashSet<>(collection.size());
	    for (PrismValue value : collection) {
	        retval.add(value.getRealValue());
	    }
	    return retval;
	}


	public static <X, V extends PrismValue> boolean containsRealValue(Collection<X> collection, V value,
			Function<X, V> valueExtractor) {
		if (collection == null) {
			return false;
		}

		for (X colVal: collection) {
			if (colVal == null) {
				return value == null;
			}

			if (valueExtractor.apply(colVal).equalsRealValue(value)) {

				return true;
			}
		}
		return false;
	}

	public static <V extends PrismValue> boolean contains(Collection<V> thisSet, V otherValue, boolean ignoreMetadata, boolean isLiteral) {
		for (V thisValue: thisSet) {
			if (thisValue.equalsComplex(otherValue, ignoreMetadata, isLiteral)) {
				return true;
			}
		}
		return false;
	}

	public static <X extends PrismValue> Collection<X> cloneValues(Collection<X> values) {
		Collection<X> clonedCollection = new ArrayList<>(values.size());
		for (X val: values) {
			clonedCollection.add((X) val.clone());
		}
		return clonedCollection;
	}

	@NotNull
	public static <T extends PrismValue> Collection<T> cloneCollectionComplex(CloneStrategy strategy, Collection<T> values) {
		Collection<T> clones = new ArrayList<>();
		if (values != null) {
			for (T value : values) {
				clones.add((T) value.cloneComplex(strategy));
			}
		}
		return clones;
	}



	public static <V extends PrismValue> boolean collectionContainsEquivalentValue(Collection<V> collection, V value) {
		if (collection == null) {
			return false;
		}
		for (V collectionVal: collection) {
			if (collectionVal.equals(value, true)) {
				return true;
			}
		}
		return false;
	}

	public static <X> Collection<PrismPropertyValue<X>> toPrismPropertyValues(PrismContext prismContext, X... realValues) {
    	Collection<PrismPropertyValue<X>> pvalues = new ArrayList<>(realValues.length);
    	for (X val: realValues) {
    		PrismUtil.recomputeRealValue(val, prismContext);
    		PrismPropertyValue<X> pval = prismContext.itemFactory().createPrismPropertyValue(val);
    		pvalues.add(pval);
    	}
    	return pvalues;
    }

	public static <O extends Objectable, C extends Containerable> Collection<PrismContainerValue<C>> toPrismContainerValues(
			Class<O> type, ItemPath path, PrismContext prismContext, C... containerValues) throws SchemaException {
    	Collection<PrismContainerValue<C>> pvalues = new ArrayList<>(containerValues.length);
    	for (C val: containerValues) {
    		prismContext.adopt(val, type, path);
    		PrismUtil.recomputeRealValue(val, prismContext);
    		PrismContainerValue<C> pval = val.asPrismContainerValue();
    		pvalues.add(pval);
    	}
    	return pvalues;
    }
}