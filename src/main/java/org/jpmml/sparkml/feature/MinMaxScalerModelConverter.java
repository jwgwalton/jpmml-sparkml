/*
 * Copyright (c) 2016 Villu Ruusmann
 *
 * This file is part of JPMML-SparkML
 *
 * JPMML-SparkML is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SparkML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SparkML.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.sparkml.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.ml.feature.MinMaxScalerModel;
import org.apache.spark.ml.linalg.Vector;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Expression;
import org.dmg.pmml.OpType;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.ValueUtil;
import org.jpmml.sparkml.FeatureConverter;
import org.jpmml.sparkml.SparkMLEncoder;
import org.jpmml.sparkml.VectorUtil;

public class MinMaxScalerModelConverter extends FeatureConverter<MinMaxScalerModel> {

	public MinMaxScalerModelConverter(MinMaxScalerModel transformer){
		super(transformer);
	}

	@Override
	public List<Feature> encodeFeatures(SparkMLEncoder encoder){
		MinMaxScalerModel transformer = getTransformer();

		double rescaleFactor = (transformer.getMax() - transformer.getMin());
		double rescaleConstant = transformer.getMin();

		Vector originalMax = transformer.originalMax();
		Vector originalMin = transformer.originalMin();

		List<Feature> features = encoder.getFeatures(transformer.getInputCol());

		VectorUtil.checkSize(features.size(), originalMax, originalMin);

		List<Feature> result = new ArrayList<>();

		for(int i = 0; i < features.size(); i++){
			Feature feature = features.get(i);

			ContinuousFeature continuousFeature = feature.toContinuousFeature();

			double max = originalMax.apply(i);
			double min = originalMin.apply(i);

			Expression expression = PMMLUtil.createApply("/", PMMLUtil.createApply("-", continuousFeature.ref(), PMMLUtil.createConstant(min)), PMMLUtil.createConstant(max - min));

			if(!ValueUtil.isOne(rescaleFactor)){
				expression = PMMLUtil.createApply("*", expression, PMMLUtil.createConstant(rescaleFactor));
			} // End if

			if(!ValueUtil.isZero(rescaleConstant)){
				expression = PMMLUtil.createApply("+", expression, PMMLUtil.createConstant(rescaleConstant));
			}

			DerivedField derivedField = encoder.createDerivedField(formatName(transformer, i), OpType.CONTINUOUS, DataType.DOUBLE, expression);

			result.add(new ContinuousFeature(encoder, derivedField));
		}

		return result;
	}
}