package codelab;

public interface DataTableInterface {

	public int getNbProp();
	public String getPropKey(int i);
	public String getPropVal(int i);
	public void updateValues();

	// Cette méthode ne doit pas apparaître dans le DataTableInterface côté plugin
	// Car c'est la méthode setSystem() qui doit être utilisée de ce côté
	public void setDashboard(DataTable dashboard);

}
