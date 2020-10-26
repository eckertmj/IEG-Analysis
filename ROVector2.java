/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author vivek.trivedi
 */
public interface ROVector2 
{
    /**
     * Get the X component of this vector
     *
     * @return The X component of this vector
     */
    public float getX();

    /**
     * Get the Y component of this vector
     *
     * @return The Y component of this vector
     */
    public float getY();

    /**
     * Get the length of this vector
     *
     * @return The length of this vector
     */
    public float length();

    /**
     * Get the dot product of this vector and another
     *
     * @param other The other vector to dot against
     * @return The dot product of the two vectors
     */
    public float dot(ROVector2 other);

    /**
     * Project this vector onto another
     *
     * @param b The vector to project onto
     * @param result The projected vector
     */
    public void projectOntoUnit(ROVector2 b, Vector2 result);

    /**
     * The length of the vector squared
     *
     * @return The length of the vector squared
     */
    public float lengthSquared();
    
}
