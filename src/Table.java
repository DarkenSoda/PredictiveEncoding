
import java.util.Vector;

public class Table {
    private Vector<Row>rows;
    public Table(){
        rows=new Vector<>();
    }
    public void setRow(Row row){
        rows.add(row);
    }
    public Vector<Row> getRows(){
        return rows;
    }
    public Row getRow(Row row){
        for(Row r:rows){
            if(r.equals(row)){
                return r;
            }
        }
        return null;
    }
    public void removeRow(Row row){
        rows.remove(row);
    }

}
