package citybites.model;

public class FoodItem {

    private int     foodId;
    private String  foodName;
    private double  price;
    private boolean available;
    private String  imagePath;  // optional; may be null

    public FoodItem() {}

    public FoodItem(int foodId, String foodName, double price, boolean available) {
        this.foodId    = foodId;
        this.foodName  = foodName;
        this.price     = price;
        this.available = available;
    }

    public int getFoodId()              { return foodId;    }
    public void setFoodId(int v)        { foodId = v;       }

    public String getFoodName()         { return foodName;  }
    public void setFoodName(String v)   { foodName = v;     }

    public double getPrice()            { return price;     }
    public void setPrice(double v)      { price = v;        }

    public boolean isAvailable()        { return available; }
    public void setAvailable(boolean v) { available = v;    }

    public String getImagePath()        { return imagePath; }
    public void setImagePath(String v)  { imagePath = v;    }
}
