//package net.java.dev.joode.util;
/**
 * A two dimensional vector
 * 
 * @author Kevin Glass
 */
public strictfp class Vector2 implements ROVector2 {
    
	/** The x component of this vector */
	public float x;
	/** The y component of this vector */
	public float y;
	
	/**
	 * Create an empty vector
	 */
	public Vector2() {
	}
	
	public final void setX(float x) {
	    this.x = x;
	}
	
	/**
	 * @see ROVector2#getX()
	 */
	public final float getX() {
		return x;
	}
	
	public final void setY(float y) {
	    this.y = y;
	}
	
	/**
	 * @see ROVector2#getY()
	 */
	public final float getY() {
		return y;
	}
	
	/**
	 * Create a new vector based on another
	 * 
	 * @param other The other vector to copy into this one
	 */
	public Vector2(ROVector2 other) {
		this(other.getX(), other.getY());
	}
    
    /**
     * Create a new vector based on another
     * 
     * @param other The other vector to copy into this one
     */
/*    public Vector2(Vector3 other) {
        this(other.getX(), other.getY());
    }*/
	
	/**
	 * Create a new vector
	 * 
	 * @param x The x component to assign
	 * @param y The y component to assign
	 */
	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Set the value of this vector
	 * 
	 * @param other The values to set into the vector
	 */
	public void set(ROVector2 other) {
		set(other.getX(), other.getY());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public float dot(ROVector2 other) {
		return (x * other.getX()) + (y * other.getY());
	}
	
	/**
	 * Set the values in this vector
	 * 
	 * @param x The x component to set
	 * @param y The y component to set
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}
    
    /**
     * Set the values in this vector
     * 
     * @param vector3 the 3-dim-vector, of which the x- and y-coordinate is written to this Vector2
     */
/*    public void set(Vector3 vector3) {
        this.x = vector3.getX();
        this.y = vector3.getY();
    }*/
	
	/**
	 * Negate this vector
	 * 
	 * @return A copy of this vector negated
	 */
	public Vector2 negate() {
		return new Vector2(-x, -y);
	}
	
	/**
	 * Add a vector to this vector
	 * 
	 * @param v The vector to add
	 */
	public void add(ROVector2 v)
	{
		x += v.getX();
		y += v.getY();
	}
	
	/**
	 * Subtract a vector from this vector
	 * 
	 * @param v The vector subtract
	 */
	public void sub(ROVector2 v)
	{
		x -= v.getX();
		y -= v.getY();
	}
	
	/**
	 * Scale this vector by a value
	 * 
	 * @param a The value to scale this vector by
	 */
	public void scale(float a)
	{
		x *= a;
		y *= a;
	}
	
	/**
	 * Normalise the vector
	 */
	public void normalise() {
		float l = length();
		
		if ( l == 0 )
			return;
		
		x /= l;
		y /= l;
	}
	
	/**
	 * The length of the vector squared
	 * 
	 * @return The length of the vector squared
	 */
	public float lengthSquared() {
		return (x * x) + (y * y);
	}
	
	/**
     * {@inheritDoc}
	 */
	public float length()
	{
		return (float) Math.sqrt(lengthSquared());
	}
	
	/**
	 * Project this vector onto another
	 * 
	 * @param b The vector to project onto
	 * @param result The projected vector
	 */
	public void projectOntoUnit(ROVector2 b, Vector2 result) {
		float dp = b.dot(this);
		
		result.x = dp * b.getX();
		result.y = dp * b.getY();
	}
	
	/**
     * {@inheritDoc}
	 */
    @Override
	public String toString() {
		return "[Vec "+x+","+y+" ("+length()+")]";
	}
    
	/**
	 * Get the distance from this point to another
	 * 
	 * @param other The other point we're measuring to
	 * @return The distance to the other point
	 */
	public float distance(ROVector2 other) {
		float dx = other.getX() - getX();
		float dy = other.getY() - getY();
		
		return (float) Math.sqrt((dx*dx)+(dy*dy));
	}
	
	/**
	 * Compare two vectors allowing for a (small) error as indicated by the delta.
	 * Note that the delta is used for the vector's components separately, i.e.
	 * any other vector that is contained in the square box with sides 2*delta and this
	 * vector at the center is considered equal.
	 * 
	 * @param other The other vector to compare this one to
	 * @param delta The allowed error
	 * @return True iff this vector is equal to other, with a tolerance defined by delta
	 */
	public boolean equalsDelta(ROVector2 other, float delta) {
		return (other.getX() - delta < x &&
				other.getX() + delta > x &&
				other.getY() - delta < y &&
				other.getY() + delta > y );
	}
	
    /**
     * returns (in the passback) a perpendicular vector.
     * Turns this vector clockwise so y becomes the x and the x becomes -y
     * 
     * @param passback
     */
    public void perp(Vector2 passback){
        passback.set(y, -x);
    }
}
