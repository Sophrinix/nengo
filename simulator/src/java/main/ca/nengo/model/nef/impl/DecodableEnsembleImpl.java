/*
The contents of this file are subject to the Mozilla Public License Version 1.1
(the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific
language governing rights and limitations under the License.

The Original Code is "DecodableEnsembleImpl.java". Description:
"Default implementation of DecodableEnsemble"

The Initial Developer of the Original Code is Bryan Tripp & Centre for Theoretical Neuroscience, University of Waterloo. Copyright (C) 2006-2008. All Rights Reserved.

Alternatively, the contents of this file may be used under the terms of the GNU
Public License license (the GPL License), in which case the provisions of GPL
License are applicable  instead of those above. If you wish to allow use of your
version of this file only under the terms of the GPL License and not to allow
others to use your version of this file under the MPL, indicate your decision
by deleting the provisions above and replace  them with the notice and other
provisions required by the GPL License.  If you do not delete the provisions above,
a recipient may use your version of this file under either the MPL or the GPL License.
*/

/*
 * Created on 20-Feb-07
 */
package ca.nengo.model.nef.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import Jama.Matrix;
import ca.nengo.dynamics.LinearSystem;
import ca.nengo.dynamics.impl.CanonicalModel;
import ca.nengo.dynamics.impl.EulerIntegrator;
import ca.nengo.dynamics.impl.LTISystem;
import ca.nengo.dynamics.impl.SimpleLTISystem;
import ca.nengo.math.ApproximatorFactory;
import ca.nengo.math.Function;
import ca.nengo.math.LinearApproximator;
import ca.nengo.math.impl.ConstantFunction;
import ca.nengo.math.impl.TimeSeriesFunction;
import ca.nengo.model.Network;
import ca.nengo.model.Node;
import ca.nengo.model.Origin;
import ca.nengo.model.RealOutput;
import ca.nengo.model.SimulationException;
import ca.nengo.model.StructuralException;
import ca.nengo.model.Termination;
import ca.nengo.model.Units;
import ca.nengo.model.impl.FunctionInput;
import ca.nengo.model.nef.DecodableEnsemble;
import ca.nengo.model.plasticity.impl.PlasticEnsembleImpl;
import ca.nengo.util.DataUtils;
import ca.nengo.util.MU;
import ca.nengo.util.Probe;
import ca.nengo.util.TimeSeries;
import ca.nengo.util.impl.TimeSeriesImpl;

/**
 * Default implementation of DecodableEnsemble.
 *
 * @author Bryan Tripp
 */
public class DecodableEnsembleImpl extends PlasticEnsembleImpl implements DecodableEnsemble {

	private static final long serialVersionUID = 1L;

	protected Map<String, DecodedOrigin> myDecodedOrigins;
	protected Map<String, DecodedTermination> myDecodedTerminations;

	private ApproximatorFactory myApproximatorFactory;
	private Map<String, LinearApproximator> myApproximators;
	private float myTime; //used to support Probeable
	
	private static Logger ourLogger = Logger.getLogger(DecodableEnsembleImpl.class);

	/**
	 * @param name Name of the Ensemble
	 * @param nodes Nodes that make up the Ensemble
	 * @param factory Source of LinearApproximators to use in decoding output
	 * @throws StructuralException if super constructor fails
	 */
	public DecodableEnsembleImpl(String name, Node[] nodes, ApproximatorFactory factory) throws StructuralException {
		super(name, nodes);

		myDecodedOrigins = new LinkedHashMap<String, DecodedOrigin>(10);
        myDecodedTerminations = new LinkedHashMap<String, DecodedTermination>(10);
		myApproximatorFactory = factory;
		myApproximators = new HashMap<String, LinearApproximator>(10);
		myTime = 0;
	}

	/**
	 * @see ca.nengo.model.nef.DecodableEnsemble#addDecodedOrigin(java.lang.String, ca.nengo.math.Function[], java.lang.String, ca.nengo.model.Network, ca.nengo.util.Probe, float, float)
	 */
    public Origin addDecodedOrigin(String name, Function[] functions, String nodeOrigin, Network environment,
			Probe probe, float startTime, float endTime) throws StructuralException, SimulationException {

	    if (myDecodedOrigins.containsKey(name)) {
            throw new StructuralException("The ensemble already contains a origin named " + name);
        }

		probe.reset();
		environment.run(startTime, endTime);
		float[] times = probe.getData().getTimes();
		float[][] evalPoints = new float[times.length][];
		for (int i = 0; i < times.length; i++) {
			evalPoints[i] = new float[]{times[i]};
		}
		float[][] values = probe.getData().getValues();
		float[][] valuesT = MU.transpose(values);

		LinearApproximator approximator = myApproximatorFactory.getApproximator(evalPoints, valuesT);
		DecodedOrigin result = new DecodedOrigin(this, name, getNodes(), nodeOrigin, functions, approximator);
		result.setMode(getMode());

        myDecodedOrigins.put(name, result);
        fireVisibleChangeEvent();
		return result;
	}

	/**
	 * @see ca.nengo.model.nef.DecodableEnsemble#addDecodedOrigin(java.lang.String, ca.nengo.math.Function[], java.lang.String, ca.nengo.model.Network, ca.nengo.util.Probe, ca.nengo.model.Termination, float[][], float)
	 */
    public Origin addDecodedOrigin(String name, Function[] functions, String nodeOrigin, Network environment,
			Probe probe, Termination termination, float[][] evalPoints, float transientTime) throws StructuralException, SimulationException {

	    if (myDecodedOrigins.containsKey(name)) {
            throw new StructuralException("The ensemble already contains a origin named " + name);
        }

		float[][] values = new float[evalPoints.length][];
		for (int i = 0; i < evalPoints.length; i++) {
			Function[] f = new Function[evalPoints[i].length];
			for (int j = 0; j < f.length; j++) {
				f[j] = new ConstantFunction(1, evalPoints[i][j]);
			}
			FunctionInput fi = new FunctionInput("DECODING SIMULATION INPUT", f, Units.UNK);
			environment.addNode(fi);
			environment.addProjection(fi.getOrigin(FunctionInput.ORIGIN_NAME), termination);
			probe.reset();
			environment.run(0, transientTime);
			TimeSeries result = probe.getData();
			environment.removeProjection(termination);
			environment.removeNode(fi.getName());

			values[i] = new float[result.getDimension()];
			int samples = (int) Math.ceil( result.getValues().length / 10d ); //use only last ~10% of run in the average to avoid transient
			for (int j = 0; j < result.getDimension(); j++) {
				values[i][j] = 0;
				for (int k = result.getValues().length - samples; k < result.getValues().length; k++) {
					values[i][j] += result.getValues()[j][k];
				}
				values[i][j] = values[i][j] / samples;
			}
		}

		LinearApproximator approximator = myApproximatorFactory.getApproximator(evalPoints, values);
		DecodedOrigin result = new DecodedOrigin(this, name, getNodes(), nodeOrigin, functions, approximator);
		result.setMode(getMode());
		myDecodedOrigins.put(name, result);
		fireVisibleChangeEvent();
		return result;
	}

	/**
	 * Lloyd Elliot's decodable origin for decoding band-limited noise using a psc optimized decoder
	 *
	 * @param name Name of decoding
	 * @param functions 1D Functions of time which represent the meaning of the Ensemble output when it runs
     *      in the Network provided (see environment arg)
	 * @param nodeOrigin The name of the Node-level Origin to decode
	 * @param environment A Network in which the Ensemble runs (may include inputs, feedback, etc)
	 * @param probe A Probe that is connected to the named Node-level Origin
	 * @param state Another probe?
	 * @param startTime Simulation time at which to start
	 * @param endTime Simulation time at which to finish
	 * @param tau Time constant
	 * @return The added Origin
	 * @throws StructuralException if origin name is taken
	 * @throws SimulationException if environment can't run
	 */
	public Origin addDecodedOrigin(String name, Function[] functions, String nodeOrigin, Network environment,
			Probe probe, Probe state, float startTime, float endTime, float tau) throws StructuralException, SimulationException {

	    if (myDecodedOrigins.containsKey(name)) {
            throw new StructuralException("The ensemble already contains a origin named " + name);
        }

		probe.reset();
		state.reset();
		environment.run(startTime, endTime);

		float [][]values;
		float []time;

		TimeSeries filtered = DataUtils.filter(probe.getData(),tau);
		values = filtered.getValues();
		time = filtered.getTimes();

		int t0 = (int)(Math.ceil(time.length/2d));
		int t1 = time.length;
		int k;

		float[][] valuesT = new float[values[0].length][t1-t0];
		TimeSeries stateData = state.getData();

		int d = stateData.getValues()[0].length;
		TimeSeriesFunction []evalPointsFunction = new TimeSeriesFunction[d];

		float [][]evalPoints = new float[t1-t0][d];

		for(int i=0;i<d;i++)
		{
			evalPointsFunction[i] = new TimeSeriesFunction(state.getData(),i);

			for(k=0;k<t1-t0;k++)
			{
				evalPoints[k][i] = evalPointsFunction[i].map(new float[]{time[k+t0]});
				for(int j=0;j<values[0].length;j++)
				{
					valuesT[j][k] = values[k+t0][j];
				}
			}
		}

		LinearApproximator approximator = myApproximatorFactory.getApproximator(evalPoints, valuesT);
		DecodedOrigin result = new DecodedOrigin(this, name, getNodes(), nodeOrigin, functions, approximator);
		result.setMode(getMode());

		myDecodedOrigins.put(name, result);
		fireVisibleChangeEvent();
		return result;
	}

    /**
     * @param name Unique name for this Termination (in the scope of this Ensemble)
     * @param matrix Transformation matrix which defines a linear map on incoming information,
     *      onto the space of vectors that can be represented by this NEFEnsemble. The first dimension
     *      is taken as matrix rows, and must have the same length as the Origin that will be connected
     *      to this Termination. The second dimension is taken as matrix columns, and must have the same
     *      length as the encoders of this NEFEnsemble. TODO: this is transposed?
     * @param tauPSC Time constant of post-synaptic current decay (all Terminations have
     *      this property but it may have slightly different interpretations depending other properties
     *      of the Termination).
     * @param isModulatory If true, inputs to this Termination do not drive Nodes in the Ensemble directly
     *      but may have modulatory influences (eg related to plasticity). If false, the transformation matrix
     *      output dimension must match the dimension of this Ensemble.
     * @return Added Termination
     * @throws StructuralException if termination name is taken
     * @see ca.nengo.model.nef.NEFEnsemble#addDecodedTermination(java.lang.String, float[][], float, boolean)
     */
    public Termination addDecodedTermination(String name, float[][] matrix, float tauPSC, boolean isModulatory)
            throws StructuralException {

        // TODO Should this also check non-decoded terminations?
        if (myDecodedTerminations.containsKey(name)) {
            throw new StructuralException("The ensemble already contains a termination named " + name);
        }

        float scale = 1 / tauPSC; //output scaling to make impulse integral = 1

        LinearSystem dynamics = new SimpleLTISystem(
                new float[]{-1f/tauPSC},
                new float[][]{new float[]{1f}},
                new float[][]{new float[]{scale}},
                new float[]{0f},
                new Units[]{Units.UNK}
        );

        EulerIntegrator integrator = new EulerIntegrator(tauPSC / 10f);

        DecodedTermination result = new DecodedTermination(this, name, matrix, dynamics, integrator);
        if (isModulatory) {
            result.setModulatory(isModulatory);
        }

        myDecodedTerminations.put(name, result);
        fireVisibleChangeEvent();
        return result;
    }

    /**
     * @param name Unique name for this Termination (in the scope of this Ensemble)
     * @param matrix Transformation matrix which defines a linear map on incoming information,
     *      onto the space of vectors that can be represented by this NEFEnsemble. The first dimension
     *      is taken as matrix rows, and must have the same length as the Origin that will be connected
     *      to this Termination. The second dimension is taken as matrix columns, and must have the same
     *      length as the encoders of this NEFEnsemble. TODO: this is transposed?
     * @param tfNumerator Coefficients of transfer function numerator (see CanonicalModel.getRealization(...)
     *      for details)
     * @param tfDenominator Coefficients of transfer function denominator
     * @param passthrough How much should pass through?
     * @param isModulatory Is the termination modulatory?
     * @return The added Termination
     * @throws StructuralException if termination name is taken
     * @see ca.nengo.model.nef.NEFEnsemble#addDecodedTermination(java.lang.String, float[][], float[], float[], float, boolean)
     */
    public Termination addDecodedTermination(String name, float[][] matrix, float[] tfNumerator, float[] tfDenominator,
            float passthrough, boolean isModulatory) throws StructuralException {

        // TODO Should this also check non-decoded terminations?
        if (myDecodedTerminations.containsKey(name)) {
            throw new StructuralException("The ensemble already contains a termination named " + name);
        }

        LTISystem dynamics = CanonicalModel.getRealization(tfNumerator, tfDenominator, passthrough);

        Matrix A = new Matrix(MU.convert(dynamics.getA(0f)));
        double[] eigenvalues = A.eig().getRealEigenvalues();
        double fastest = Math.abs(eigenvalues[0]);
        for (int i = 1; i < eigenvalues.length; i++) {
            if (Math.abs(eigenvalues[i]) > fastest) {
                fastest = Math.abs(eigenvalues[i]);
            }
        }

        EulerIntegrator integrator = new EulerIntegrator(1f / (10f * (float) fastest));

        DecodedTermination result = new DecodedTermination(this, name, matrix, dynamics, integrator);
        if (isModulatory) {
            result.setModulatory(isModulatory);
        }

        myDecodedTerminations.put(name, result);
        fireVisibleChangeEvent();
        return result;
    }

    /**
     * @see ca.nengo.model.nef.NEFEnsemble#removeDecodedTermination(java.lang.String)
     */
    public DecodedTermination removeDecodedTermination(String name) throws StructuralException {
        if (myDecodedTerminations.containsKey(name)) {
            DecodedTermination result = myDecodedTerminations.remove(name);
            fireVisibleChangeEvent();

            return result;
        }

        throw new StructuralException("Termination " + name +
                " does not exist or not a DecodedTermination");
    }

    /**
     * @see ca.nengo.model.nef.NEFEnsemble#removeDecodedTermination(java.lang.String)
     */
    public DecodedOrigin removeDecodedOrigin(String name) throws StructuralException {
        if (myDecodedOrigins.containsKey(name)) {
            DecodedOrigin result = myDecodedOrigins.remove(name);
            fireVisibleChangeEvent();

            return result;
        }

        throw new StructuralException("Origin " + name +
                " does not exist or not a DecodedOrigin");
    }

    /**
     * Used to get decoded terminations to give to GPU.
     * @return all DecodedTerminations
     */
    public DecodedTermination[] getDecodedTerminations(){
        return myDecodedTerminations.values().toArray(new DecodedTermination[0]);
        //return (OrderedTerminations != null) ? (DecodedTermination[])OrderedTerminations.toArray(new DecodedTermination[0]) : new DecodedTermination[0];
    }

	/**
	 * @see ca.nengo.model.nef.DecodableEnsemble#doneOrigins()
	 */
    public void doneOrigins() {
		myApproximators.clear();
	}

	/**
	 * @see ca.nengo.model.Node#getOrigin(java.lang.String)
	 */
	@Override
    public Origin getOrigin(String name) throws StructuralException {
		return myDecodedOrigins.containsKey(name) ?
		        myDecodedOrigins.get(name) : super.getOrigin(name);
	}

    /**
     * @see ca.nengo.model.Node#getTermination(java.lang.String)
     */
    @Override
    public Termination getTermination(String name) throws StructuralException {
        return myDecodedTerminations.containsKey(name) ?
                myDecodedTerminations.get(name) : super.getTermination(name);
    }

	/**
	 * @see ca.nengo.model.Ensemble#getOrigins()
	 */
	@Override
    public Origin[] getOrigins() {
        ArrayList<Origin> result = new ArrayList<Origin>(10);
        Origin[] composites = super.getOrigins();
        for (Origin composite : composites) {
            result.add(composite);
        }

        // getOrigins is called by NEFEnsembleImpl in the constructor
        if (myDecodedOrigins == null) {
            return result.toArray(new Origin[0]);
        }


        for (Origin o : myDecodedOrigins.values()) {
            result.add(o);
        }
        return result.toArray(new Origin[0]);
	}

    /**
     * Used to get decoded origins to give to GPU.
     * @return All DecodedOrigins
     */
    public DecodedOrigin[] getDecodedOrigins(){
        ArrayList<DecodedOrigin> result = new ArrayList<DecodedOrigin>(10);

        for (DecodedOrigin o : myDecodedOrigins.values()) {
            result.add(o);
        }
        return result.toArray(new DecodedOrigin[0]);
    }

    /**
     * @see ca.nengo.model.Ensemble#getTerminations()
     */
    @Override
    public Termination[] getTerminations() {
        ArrayList<Termination> result = new ArrayList<Termination>(10);
        Termination[] composites = super.getTerminations();
        for (Termination composite : composites) {
            result.add(composite);
        }

        for (Termination t : myDecodedTerminations.values()) {
            result.add(t);
        }
        return result.toArray(new Termination[0]);
    }

	/**
	 * @see ca.nengo.model.Node#run(float, float)
	 */
	@Override
    public void run(float startTime, float endTime) throws SimulationException {
		super.run(startTime, endTime);

		for (DecodedOrigin o : myDecodedOrigins.values()) {
            o.run(null, startTime, endTime);
        }

		setTime(endTime);
	}

	/**
	 * Allows subclasses to set the simulation time, which is used to support Probeable.
	 * This is normally set in the run() method. Subclasses that override run() without
	 * calling it should set the time.
	 *
	 * @param time Simulation time
	 */
	public void setTime(float time) {
		myTime = time;
	}

	/**
	 * @return The source of LinearApproximators for this ensemble (used to find linear decoding vectors).
	 */
	public ApproximatorFactory getApproximatorFactory() {
		return myApproximatorFactory;
	}

	/**
	 * @see ca.nengo.model.Probeable#getHistory(java.lang.String)
	 */
    @Override
    public TimeSeries getHistory(String stateName) throws SimulationException {
		TimeSeries result = null;

		Origin origin = myDecodedOrigins.get(stateName);
		DecodedTermination t = myDecodedTerminations.get(stateName);

		if (origin != null) {
			if (t != null)
				ourLogger.warn("Warning, probe set on ensemble with matching origin/termination names (\"" + 
						stateName + "\"), probing origin by default");
			
		    origin.setRequiredOnCPU(true);
			float[] vals = ((RealOutput) origin.getValues()).getValues();
			Units[] units = new Units[vals.length];
			for (int i = 0; i < vals.length; i++) {
				units[i] = origin.getValues().getUnits();
			}
			result = new TimeSeriesImpl(new float[]{myTime}, new float[][]{vals}, units);
		} else if (t != null) {
			result = t.getHistory(DecodedTermination.OUTPUT);
    	} else if (t == null && stateName.endsWith(":STP")) {
                String originName = stateName.substring(0,stateName.length()-4);
                try {
                    DecodedOrigin o = (DecodedOrigin) getOrigin(originName);
                    result = o.getSTPHistory();
                } catch (StructuralException e) {
                    throw new SimulationException(e);
                }
		} else {
		    result = super.getHistory(stateName);
		}

		return result;
	}

	/**
	 * @see ca.nengo.model.Probeable#listStates()
	 */
	@Override
    public Properties listStates() {
		Properties result = super.listStates();

		Iterator<String> it = myDecodedTerminations.keySet().iterator();
        while (it.hasNext()) {
            String termName = it.next().toString();
            result.setProperty(termName, "Output of Termination " + termName);
        }
		
		it = myDecodedOrigins.keySet().iterator();
		while (it.hasNext()) {
			String name = it.next().toString();
			result.setProperty(name, "Function of NEFEnsemble state"); //TODO: could put function.toString() here
		}

		return result;
	}

	public void stopProbing(String stateName){
		Origin origin = myDecodedOrigins.get(stateName);
		
		if (origin != null) {
		    origin.setRequiredOnCPU(false);
		}
	}
	
	@Override
	public DecodableEnsembleImpl clone() throws CloneNotSupportedException {
		DecodableEnsembleImpl result = (DecodableEnsembleImpl) super.clone();

		result.myApproximatorFactory = myApproximatorFactory.clone();
		result.myApproximators = new HashMap<String, LinearApproximator>(5);
		result.myDecodedOrigins = new LinkedHashMap<String,DecodedOrigin>(10);
		for (DecodedOrigin oldOrigin : myDecodedOrigins.values()) {
			Function[] oldFunctions = oldOrigin.getFunctions();
			Function[] newFunctions = new Function[oldFunctions.length];
			for (int i = 0; i < newFunctions.length; i++) {
				newFunctions[i] = oldFunctions[i].clone();
			}

			try {
				// FIXME: this should use DecodedOrigin.clone(Node)
				DecodedOrigin newOrigin = new DecodedOrigin(
						result,
						oldOrigin.getName(),
						result.getNodes(),
						oldOrigin.getNodeOrigin(),
						newFunctions,
						MU.clone(oldOrigin.getDecoders()));
				if (oldOrigin.getNoise() != null) {
                    newOrigin.setNoise(oldOrigin.getNoise());
                }
				newOrigin.setMode(oldOrigin.getMode());
				result.myDecodedOrigins.put(newOrigin.getName(), newOrigin);
				newOrigin.reset(false);
			} catch (StructuralException e) {
				throw new CloneNotSupportedException("Error cloning DecodedOrigin: " + e.getMessage());
			}
		}
		
        result.myDecodedTerminations = new LinkedHashMap<String,DecodedTermination>(10);
        for (String key : myDecodedTerminations.keySet()) {
            DecodedTermination t = myDecodedTerminations.get(key).clone(result);
            result.myDecodedTerminations.put(key, t);
        }

        //change scaling terminations references to the new copies
        for (String key : result.myDecodedTerminations.keySet()) {
            DecodedTermination t = result.myDecodedTerminations.get(key);
            if (t.getScaling() != null) {
                t.setScaling(result.myDecodedTerminations.get(t.getScaling().getName()));
            }
        }

		return result;
	}
	
	public void reset(boolean randomize)
	{
		super.reset(randomize);
		
		for (DecodedTermination termination : myDecodedTerminations.values()) {
            termination.reset(randomize);
		}

		for (DecodedOrigin origin : myDecodedOrigins.values()) {
			origin.reset(randomize);
		}
	}

}
