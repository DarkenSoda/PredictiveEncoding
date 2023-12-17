
import java.util.Vector;

public class Table {
    private Vector<Row>rows;
    public Table(){
        rows=new Vector<>();
    }
    public void addRow(Row row){
        rows.add(row);
    }
    public Vector<Row> getRows(){
        return rows;
    }
    public void removeRow(Row row){
        rows.remove(row);
    }

}
