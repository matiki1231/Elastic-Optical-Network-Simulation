package mtk.eon.net;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Consumer;

import mtk.eon.net.demand.Demand;
import mtk.eon.net.spectrum.BackupSpectrumSegment;
import mtk.eon.net.spectrum.Spectrum;
import mtk.eon.net.spectrum.WorkingSpectrumSegment;

/**
 * Used for calculation of metrics in each segment of network path
 * @author Michal
 *
 */
public class PartedPath implements Comparable<PartedPath>, Iterable<PathPart> {

	NetworkPath path;
	boolean isUp;
	ArrayList<PathPart> parts = new ArrayList<PathPart>();
	double occupiedRegeneratorsPercentage, occupiedCPU, occupiedMemory, occupiedStorage;
	double metric = -1.0;
	// TODO Use properties here and in other objects
	
	public PartedPath(Network network, NetworkPath path, boolean isUp) {
		int allRegenerators = 0, allCPU = 0, allMemory = 0, allStorage = 0;
		for (int i = 1; i < path.size(); i++) {
			NetworkNode source = path.get(isUp ? i - 1 : path.size() - i);
			NetworkNode destination = path.get(isUp ? i : path.size() - i - 1);
			if (i > 1) {
				occupiedRegeneratorsPercentage += source.occupiedRegenerators;
				allRegenerators += source.regeneratorsCount;
				occupiedCPU += source.occupiedCPU;
				occupiedStorage += source.occupiedStorage;
				occupiedMemory += source.occupiedMemory;
				allMemory += source.memory;
				allCPU += source.cpu;
				allStorage += source.storage;
			}
			parts.add(new PathPart(source, destination, network.getLink(source, destination).getLength(), 
					network.getLinkSlices(source, destination)));
		}
		if (allRegenerators != 0){
			occupiedRegeneratorsPercentage /= allRegenerators;
			occupiedCPU /= allCPU;
			occupiedMemory /= allMemory;
			occupiedStorage /= allStorage;
		} else{
			occupiedRegeneratorsPercentage = 1; // TODO TO JEST REGENERATOROWA KUPA...
			occupiedCPU = 1;
			occupiedMemory = 1;
			occupiedStorage = 1;
		}
		this.isUp = isUp;
		this.path = path;
	}
	
	public void calculateMetricFromParts() {
		metric = 0.0;
		for (PathPart part : parts) metric += part.metric;
		metric /= parts.size();
	}
	
	public void setMetric(double metric) {
		this.metric = metric;
	}
	
	public double getMetric() {
		return metric;
	}
	
	public void mergeRegeneratorlessParts() {
		for (int i = 1; i < parts.size(); i++)
			if (!parts.get(i).getSource().hasFreeRegenerators()) {
				parts.get(i - 1).merge(parts.get(i));
				parts.remove(i);
				i--;
			}
	}
	
	public void mergeIdenticalModulation(Network network, int volume) {
		for (int i = 1; i < parts.size(); i++)
			if (parts.get(i - 1).getModulation() == parts.get(i).getModulation() && parts.get(i - 1).getLength() +
					parts.get(i).getLength() <= parts.get(i).getModulation().modulationDistances[volume]) {
				parts.get(i - 1).merge(parts.get(i));
				parts.remove(i);
				i--;
			}
	}
	
	public Modulation getModulationFromLongestPart() {
		PathPart longestPart = parts.get(0), part;
		for (int i = 1; i < parts.size(); i++) {
			part = parts.get(i);
			if (longestPart.getLength() < part.getLength()) longestPart = part;
		}
		return longestPart.getModulation();
	}
	
	public double getOccupiedRegeneratorsPercentage() {
		return occupiedRegeneratorsPercentage;
	}
	
	public int getNeededRegeneratorsCount() {
		return parts.size() - 1;
	}
	
	public boolean isDisjoint(PartedPath path) {
		return this.path.isDisjoint(path);
	}
	
	public boolean allocate(Network network, Demand demand) {
		for (PathPart part : parts) {
			if (part != parts.get(0)){
				part.source.occupyRegenerators(1, false);
				part.source.occupyCpu(demand.getCPU(), false);
				part.source.occupyMemory(demand.getMemory(), false);
				part.source.occupyStorage(demand.getStorage(), false);
			}
			
		}
		
		for (PathPart part : parts) {
			Spectrum slices = part.getSlices();
			int slicesCount, offset;
			if (demand.getWorkingPath() == null) {
				slicesCount = part.getModulation().slicesConsumption[(int) Math.ceil(demand.getVolume() / 10) - 1];
				offset = slices.canAllocateWorking(slicesCount);
				if (offset == -1) return false;
				part.segment = new WorkingSpectrumSegment(offset, slicesCount, demand);
			} else {
				slicesCount = part.getModulation().slicesConsumption[(int) Math.ceil(demand.getSqueezedVolume() / 10) - 1];
				offset = slices.canAllocateBackup(demand, slicesCount);
				if (offset == -1) return false;
				part.segment = new BackupSpectrumSegment(offset, slicesCount, demand);
			}
			for	(Spectrum slice : part.spectra) slice.allocate(part.segment);
		}

		return true;
	}
	
	public void toWorking(Demand demand) {
		for (PathPart part : parts) {
			part.segment = new WorkingSpectrumSegment(part.segment.getRange(), demand);
			for	(Spectrum slices : part.spectra) slices.claimBackup(demand);
		}
	}
	
	public void deallocate(Demand demand) {
		for (PathPart part : parts) {
			if (part != parts.get(0)){
				part.source.occupyRegenerators(-1, true);
				part.source.occupyCpu(-demand.getCPU(), true);
				part.source.occupyMemory(-demand.getMemory(), true);
				part.source.occupyStorage(-demand.getStorage(), true);
			}
			for	(Spectrum slices : part.spectra) slices.deallocate(demand);
		}
	}

	public int getPartsCount() {
		return parts.size();
	}
	
	public void forEachNode(Consumer<NetworkNode> action) {
		if (isUp)
			for (int i = 0; i < path.size(); i++)
				action.accept(path.get(i));
		else
			for (int i = 0; i < path.size(); i++)
				action.accept(path.get(path.size() - 1 - i));
	}
	
	public NetworkPath getPath() {
		return path;
	}
	
	@Override
	public Iterator<PathPart> iterator() {
		return parts.iterator();
	}

	@Override
	public int compareTo(PartedPath other) {
		if (metric < other.metric) return -1;
		else if (metric == other.metric) return 0;
		else return 1;
	}
	
	private static class PartedPathLengthComparator implements Comparator<PartedPath> {

		@Override
		public int compare(PartedPath path1, PartedPath path2) {
			return path1.path.compareTo(path2.path);
		}
	}
	
	public static final PartedPathLengthComparator LENGTH_COMPARATOR = new PartedPathLengthComparator();
}
