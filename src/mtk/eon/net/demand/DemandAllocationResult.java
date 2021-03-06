package mtk.eon.net.demand;

import mtk.eon.net.PartedPath;

public class DemandAllocationResult {

	public static enum Type {
		SUCCESS, NO_REGENERATORS, NO_SPECTRUM, NO_CPU, NO_MEMORY, NO_STORAGE;
	}
	
	public static final DemandAllocationResult NO_REGENERATORS = new DemandAllocationResult(Type.NO_REGENERATORS);
	public static final DemandAllocationResult NO_SPECTRUM = new DemandAllocationResult(Type.NO_SPECTRUM);
	public static final DemandAllocationResult NO_CPU = new DemandAllocationResult(Type.NO_CPU);
	public static final DemandAllocationResult NO_MEMORY = new DemandAllocationResult(Type.NO_MEMORY);
	public static final DemandAllocationResult NO_STORAGE = new DemandAllocationResult(Type.NO_STORAGE);	
	
	public final Type type;
	public final PartedPath workingPath;
	public final PartedPath backupPath;
	
	DemandAllocationResult(Type type) {
		this.type = type;
		workingPath = null;
		backupPath = null;
	}
	
	public DemandAllocationResult(Type type, PartedPath workingPath) {
		this.type = Type.SUCCESS;
		this.workingPath = workingPath;
		this.backupPath = null;
	}
	
	public DemandAllocationResult(PartedPath workingPath, PartedPath backupPath) {
		this.type = Type.SUCCESS;
		this.workingPath = workingPath;
		this.backupPath = backupPath;
	}
}
