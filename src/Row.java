public class Row {
    private int start;
    private int end;
    private int q;
    private int q_;
    public Row()
    {
        start = 0;
        end = 0;
        q = 0;
        q_ = 0;
    }
    // Constructor for creating a new row object.
    public Row(int start, int end, int q, int q_) {
        this.start = start;
        this.end = end;
        this.q = q;
        this.q_ = q_;
    }
    //getter setter
    public int getStart() {
        return start;
    }
    public void setStart(int start) {
        this.start = start;
    }
    public int getEnd() {
        return end;
    }
    public void setEnd(int end) {
        this.end = end;
    }
    public int getQ() {
        return q;
    }
    public void setQ(int q) {
        this.q = q;
    }
    public int getQ_() {
        return q_;
    }
    public void setQ_(int q_) {
        this.q_ = q_;
    }
       
    

}
