package org.openml.webapplication.fantail;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.openml.apiconnector.xml.DataQuality.Quality;
import org.openml.webapplication.fantail.dc.Characterizer;
import org.openml.webapplication.features.CharacterizerFactory;
import org.openml.webapplication.features.FantailConnector;
import org.openml.webapplication.testutils.DatasetFactory;

import weka.core.Instances;

public class TestFantailConnector {
	
	private static Method makeMethodAccessible() throws NoSuchMethodException, SecurityException {
		Class[] cArgClasses = new Class[6];
        cArgClasses[0] = int.class;
        cArgClasses[1] = Instances.class;
        cArgClasses[2] = List.class;
        cArgClasses[3] = List.class;
        cArgClasses[4] = String.class;
        cArgClasses[5] = List.class;
        
        Method method = FantailConnector.class.getDeclaredMethod("extractFeatures", cArgClasses);
		method.setAccessible(true);
		return method;
	}
	
	@Test
	public final void testXorNumeric() throws Exception {
		List<Characterizer> characterizers = CharacterizerFactory.all(null);
		FantailConnector connector = new FantailConnector(null, characterizers);
        
        String[] target = {"y"};
        Object[] cArgValues = new Object[6];
        cArgValues[0] = -1;
        cArgValues[1] = DatasetFactory.getXORNumericNoClass();
        cArgValues[2] = new ArrayList<String>(Arrays.asList(target));
        cArgValues[3] = null;
        cArgValues[4] = null;
        cArgValues[5] = new ArrayList<String>(Arrays.asList(new String[0]));
		
		Method method = makeMethodAccessible();
		
		Set<Quality> result = (Set<Quality>) method.invoke(connector, cArgValues);
		
		assertEquals(CharacterizerFactory.getExpectedQualities(characterizers).size(), result.size());
	}
	
	@Test
	public final void testXorNominal() throws Exception {
		List<Characterizer> characterizers = CharacterizerFactory.all(null);
		FantailConnector connector = new FantailConnector(null, characterizers);
        
        String[] target = {"class"};
        Object[] cArgValues = new Object[6];
        cArgValues[0] = -1;
        cArgValues[1] = DatasetFactory.getXORNominalNoClass();
        cArgValues[2] = new ArrayList<String>(Arrays.asList(target));
        cArgValues[3] = null;
        cArgValues[4] = null;
        cArgValues[5] = new ArrayList<String>(Arrays.asList(new String[0]));
		
		Method method = makeMethodAccessible();
		
		Set<Quality> result = (Set<Quality>) method.invoke(connector, cArgValues);
		
		assertEquals(CharacterizerFactory.getExpectedQualities(characterizers).size(), result.size());
	}
	
	
}
