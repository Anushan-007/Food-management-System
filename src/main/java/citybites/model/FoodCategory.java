package citybites.model;

public class FoodCategory {

    private int    categoryId;
    private String categoryName;
    private String description;

    public FoodCategory() {}

    public FoodCategory(int categoryId, String categoryName) {
        this.categoryId   = categoryId;
        this.categoryName = categoryName;
    }

    public FoodCategory(int categoryId, String categoryName, String description) {
        this.categoryId   = categoryId;
        this.categoryName = categoryName;
        this.description  = description;
    }

    public int    getCategoryId()           { return categoryId;   }
    public void   setCategoryId(int v)      { categoryId = v;      }
    public String getCategoryName()         { return categoryName; }
    public void   setCategoryName(String v) { categoryName = v;    }
    public String getDescription()          { return description;  }
    public void   setDescription(String v)  { description = v;     }

    @Override
    public String toString() { return categoryName; }
}
