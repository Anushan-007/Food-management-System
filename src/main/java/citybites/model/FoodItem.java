package citybites.model;

public class FoodItem {

    private int     foodId;
    private String  foodName;
    private double  price;
    private boolean available;
    private int     stockQuantity;
    private String  imagePath;
    private int     categoryId;
    private String  categoryName;

    public FoodItem() {}

    public FoodItem(int foodId, String foodName, double price, boolean available) {
        this.foodId    = foodId;
        this.foodName  = foodName;
        this.price     = price;
        this.available = available;
    }

    public int     getFoodId()               { return foodId;        }
    public void    setFoodId(int v)          { foodId = v;           }
    public String  getFoodName()             { return foodName;      }
    public void    setFoodName(String v)     { foodName = v;         }
    public double  getPrice()                { return price;         }
    public void    setPrice(double v)        { price = v;            }
    public boolean isAvailable()             { return available;     }
    public void    setAvailable(boolean v)   { available = v;        }
    public int     getStockQuantity()        { return stockQuantity; }
    public void    setStockQuantity(int v)   { stockQuantity = v;    }
    public String  getImagePath()            { return imagePath;     }
    public void    setImagePath(String v)    { imagePath = v;        }
    public int     getCategoryId()           { return categoryId;    }
    public void    setCategoryId(int v)      { categoryId = v;       }
    public String  getCategoryName()         { return categoryName;  }
    public void    setCategoryName(String v) { categoryName = v;     }

    @Override
    public String toString() {
        return foodName;
    }
}
