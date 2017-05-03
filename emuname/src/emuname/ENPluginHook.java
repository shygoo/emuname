package net.shygoo.emuname;

public class ENPluginHook {
	private int[] ids; // unique identifiers for method removal
	private int[] orders; // execution orders
	private ENMethod[] methods; // methods
	
	private int size = 0; // method count
	private int nextId = 0; // next id to assign

	public ENPluginHook(){

	}
	
	public int addMethod(ENMethod method, int order){
		int id = getNextId();
		// new arrays which current data will be copied to
		int nSize = size + 1;
		int[] nOrders = new int[nSize];
		ENMethod[] nMethods = new ENMethod[nSize];
		int[] nIds = new int[nSize];
		int i = 0, j = 0; // i = old data index, j = new data index
		// copy data until insertion point is found
		for(; i < size && orders[i] <= order; i++){
			nOrders[j] = orders[i];
			nMethods[j] = methods[i];
			nIds[j] = ids[i];
			j++;
		}
		// insert new method
		nOrders[j] = order; 
		nMethods[j] = method;
		nIds[j] = id;
		j++;
		// continue copying
		for(; i < size; i++){
			nOrders[j] = orders[i];
			nMethods[j] = methods[i];
			nIds[j] = ids[i];
			j++;
		}
		// replace old data with new data
		orders = nOrders;
		methods = nMethods;
		ids = nIds;
		size = nSize;
		return id;
	}
	
	public int addMethod(ENMethod method){
		return addMethod(method, getLastOrder());
	}
	
	public void removeMethod(int id){
		// new arrays which current data will be copied to
		int nSize = size - 1;
		int[] nOrders = new int[size - 1];
		ENMethod[] nMethods = new ENMethod[size - 1];
		int[] nIds = new int[size - 1];
		int i = 0, j = 0;
		// copy until method to remove is found
		for(; i < size && ids[i] != id; i++){
			nOrders[j] = orders[i];
			nMethods[j] = methods[i];
			nIds[j] = ids[i];
			j++;
		}
		i++; // move to next element without copying
		// copy the rest
		for(; i < size; i++){
			nOrders[j] = orders[i];
			nMethods[j] = methods[i];
			nIds[j] = ids[i];
			j++;
		}
		orders = nOrders;
		methods = nMethods;
		ids = nIds;
	}
	
	public int getLastOrder(){
		return (size == 0) ? 0 : orders[size - 1];
	}
	
	public void invokeMethods(ENEvent event){
		for(int i = 0; i < size; i++){
			methods[i].run(event);
		}
	}
	
	private int getNextId(){
		return nextId++;
	}
}