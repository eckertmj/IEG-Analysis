/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.Graphics;
//import java.awt.Graphics2D;
//import java.awt.geom.Point2D;

import java.awt.Point;
import java.util.*;

/**
 *
 * @author vivek.trivedi - ref from Shyam Prasad Murarka and T. Rice
 */
public class Geometry_Helper 
{
        public IntersectionObject m_result = null;
        
        public static void drawDiamond(Graphics g, int x1, int y1, int x2, int y2)  
        {  
            double x = (x1+x2)/2;  
            double y = (y1+y2)/2;  
            g.drawLine(x1, (int)y , (int)x , y1);  
            g.drawLine((int)x , y1, x2, (int)y );  
            g.drawLine(x2, (int)y , (int)x , y2);  
            g.drawLine((int)x , y2, x1, (int)y );  
        }           

        public class IntSecPoints
        {
            public double p1 = 0.0;
            public double p2 = 0.0;

            public IntSecPoints() 
            {
                p1 = p2 = 0.0;
            }
            public IntSecPoints (double _p1, double _p2)
            {
                p1 = _p1;
                p2 = _p2;
            }
        }
        
        
        public class IntersectionObject
        {
            public ArrayList<Vector2> points = new ArrayList<Vector2>();

            public void InsertSolution(float x_, float y_)
            {
                points.add(new Vector2(x_, y_));
            }

            public void InsertSolution(Vector2 v_)
            {
                points.add(v_);
            }

            public int NumberOfSolutions()
            {
                return points.size();
            }
        }
        
        public IntersectionObject CircleToCircleIntersection(float x0_, float y0_, float r0_, 
                                                                    float x1_, float y1_, float r1_)
        {
            m_result = new IntersectionObject();
            float a, dist, h;
            Vector2 d, r = new Vector2(), v2 = new Vector2();

            //d is the vertical and horizontal distances between the circle centers
            d = new Vector2(x1_ - x0_, y1_ - y0_);

            //distance between the circles
            dist = d.length();

            //Check for equality and infinite intersections exist
            if (dist == 0 && r0_ == r1_)
            {
                return m_result;
            }

            //Check for solvability
            if (dist > r0_ + r1_)
            {
                //no solution. circles do not intersect
                return m_result;
            }
            if (dist < Math.abs(r0_ - r1_))
            {
                //no solution. one circle is contained in the other
                return m_result;
            }
            if (dist == r0_ + r1_)
            {
                //one solution
                m_result.InsertSolution((x0_ - x1_) / (r0_ + r1_) * r0_ + x1_, (y0_ - y1_) / (r0_ + r1_) * r0_ + y1_);
                return m_result;
            }

            /* 'point 2' is the point where the line through the circle
             * intersection points crosses the line between the circle
             * centers.  
             */

            //Determine the distance from point 0 to point 2
            a = ((r0_ * r0_) - (r1_ * r1_) + (dist * dist)) / (2.0f * dist);

            //Determine the coordinates of point 2
            v2 = new Vector2(x0_ + (d.x * a / dist), y0_ + (d.y * a / dist));

            //Determine the distance from point 2 to either of the intersection points
            h = (float)Math.sqrt((r0_ * r0_) - (a * a));

            //Now determine the offsets of the intersection points from point 2
            r = new Vector2(-d.y * (h / dist), d.x * (h / dist));

            //Determine the absolute intersection points
            
            Vector2 t = new Vector2();
            t.x = v2.x + r.x;
            t.y = v2.y + r.y;
            m_result.InsertSolution(t);

            Vector2 t1 = new Vector2();
            t.x = v2.x - r.x;
            t.y = v2.y - r.y;
            m_result.InsertSolution(t1);

            return m_result;
        }        

      //Circle to Line
        public IntersectionObject CircleToLineIntersection(float x1_, float y1_, float r1_, 
                                                                  float x2_, float y2_, float x3_, float y3_)
        {
            return LineToCircleIntersection(x2_, y2_, x3_, y3_, x1_, y1_, r1_);
        }

        //Line to Circle
        public IntersectionObject LineToCircleIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                                  float x3_, float y3_, float r3_)
        {
            m_result = new IntersectionObject();
            Vector2 v1, v2;
            //Vector from point 1 to point 2
            v1 = new Vector2(x2_ - x1_, y2_ - y1_);
            //Vector from point 1 to the circle's center
            v2 = new Vector2(x3_ - x1_, y3_ - y1_);

            float dot = v1.x * v2.x + v1.y * v2.y;
            Vector2 proj1 = new Vector2(((dot / (v1.lengthSquared())) * v1.x), ((dot / (v1.lengthSquared())) * v1.y));
            Vector2 midpt = new Vector2(x1_ + proj1.x, y1_ + proj1.y);

            float distToCenter = (midpt.x - x3_) * (midpt.x - x3_) + (midpt.y - y3_) * (midpt.y - y3_);
            if (distToCenter > r3_ * r3_) return m_result;
            
            if (distToCenter == r3_ * r3_)
            {
                m_result.InsertSolution(midpt);
                return m_result;
            }
            float distToIntersection;
            if (distToCenter == 0)
            {
                distToIntersection = r3_;
            }
            else
            {
                distToCenter = (float)Math.sqrt(distToCenter);
                distToIntersection = (float)Math.sqrt(r3_ * r3_ - distToCenter * distToCenter);
            }
            float lineSegmentLength = 1 / (float)v1.length();
            v1.x *= lineSegmentLength;
            v1.y *= lineSegmentLength;

            v1.x *= distToIntersection;
            v1.y *= distToIntersection;


            Vector2 t = new Vector2();
            t.x = v2.x + midpt.x;
            t.y = v2.y + midpt.y;
            
            m_result.InsertSolution(t);

            Vector2 t1 = new Vector2();
            t.x = v2.x - midpt.x;
            t.y = v2.y -midpt.y;
            m_result.InsertSolution(t1);
            

            return m_result;
        }

        //Circle to LineSegment
        public IntersectionObject CircleToLineSegmentIntersection(float x1_, float y1_, float r1_, 
                                                                         float x2_, float y2_, float x3_, float y3_)
        {
            return LineSegmentToCircleIntersection(x2_, y2_, x3_, y3_, x1_, y1_, r1_);
        }
        
        //LineSegment to Circle
        public IntersectionObject LineSegmentToCircleIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                                         float x3_, float y3_, float r3_)
        {
            m_result = new IntersectionObject();
            Vector2 v1, v2;
            //Vector from point 1 to point 2
            v1 = new Vector2(x2_ - x1_, y2_ - y1_);
            //Vector from point 1 to the circle's center
            v2 = new Vector2(x3_ - x1_, y3_ - y1_);

            float dot = v1.x * v2.x + v1.y * v2.y;
            Vector2 proj1 = new Vector2(((dot / (v1.lengthSquared())) * v1.x), ((dot / (v1.lengthSquared())) * v1.y));

            Vector2 midpt = new Vector2(x1_ + proj1.x, y1_ + proj1.y);
            float distToCenter = (midpt.x - x3_) * (midpt.x - x3_) + (midpt.y - y3_) * (midpt.y - y3_);
            if (distToCenter > r3_ * r3_) 
                return m_result;
            if (distToCenter == r3_ * r3_)
            {
                m_result.InsertSolution(midpt);
                return m_result;
            }
            
            float distToIntersection;
            if (distToCenter == 0)
            {
                distToIntersection = r3_;
            }
            else
            {
                distToCenter = (float)Math.sqrt(distToCenter);
                distToIntersection = (float)Math.sqrt(r3_ * r3_ - distToCenter * distToCenter);
            }
            float lineSegmentLength = 1 / (float)v1.length();
            v1.x *= lineSegmentLength;
            v1.y *= lineSegmentLength;

            v1.x *= distToIntersection;
            v1.y *= distToIntersection;

            Vector2 solution1 = new Vector2();
            solution1.x =         midpt.x + v1.x;
            solution1.y =         midpt.y + v1.y;
            
            if ((solution1.x - x1_) * v1.x + (solution1.y - y1_) * v1.y > 0 &&
                (solution1.x - x2_) * v1.x + (solution1.y - y2_) * v1.y < 0)
            {
                m_result.InsertSolution(solution1);
            }
            Vector2 solution2 = new Vector2();
            solution2.x =midpt.x - v1.x;
            solution2.y =midpt.y - v1.y;
            if ((solution2.x - x1_) * v1.x + (solution2.y - y1_) * v1.y > 0 &&
                (solution2.x - x2_) * v1.x + (solution2.y - y2_) * v1.y < 0)
            {
                m_result.InsertSolution(solution2);
            }
            return m_result;
        }

        //Circle to Ray
        public IntersectionObject CircleToRayIntersection(float x1_, float y1_, float r1_, 
                                                                 float x2_, float y2_, float x3_, float y3_)
        {
            return RayToCircleIntersection(x2_, y2_, x3_, y3_, x1_, y1_, r1_);
        }

        //Ray to Circle
        public IntersectionObject RayToCircleIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                                 float x3_, float y3_, float r3_)
        {
            m_result = new IntersectionObject();
            Vector2 v1, v2;
            //Vector from point 1 to point 2
            v1 = new Vector2(x2_ - x1_, y2_ - y1_);
            //Vector from point 1 to the circle's center
            v2 = new Vector2(x3_ - x1_, y3_ - y1_);

            float dot = v1.x * v2.x + v1.y * v2.y;
            Vector2 proj1 = new Vector2(((dot / (v1.lengthSquared())) * v1.x), ((dot / (v1.lengthSquared())) * v1.y));

            Vector2 midpt = new Vector2(x1_ + proj1.x, y1_ + proj1.y);
            float distToCenter = (midpt.x - x3_) * (midpt.x - x3_) + (midpt.y - y3_) * (midpt.y - y3_);
            
            if (distToCenter > r3_ * r3_) return m_result;
            
            if (distToCenter == r3_ * r3_)
            {
                m_result.InsertSolution(midpt);
                return m_result;
            }
            
            float distToIntersection;
            
            if (distToCenter == 0)
            {
                distToIntersection = r3_;
            }
            else
            {
                distToCenter = (float)Math.sqrt(distToCenter);
                distToIntersection = (float)Math.sqrt(r3_ * r3_ - distToCenter * distToCenter);
            }
            
            float lineSegmentLength = 1 / (float)v1.length();
            
            v1.x *= lineSegmentLength;
            v1.y *= lineSegmentLength;

            v1.x *= distToIntersection;
            v1.y *= distToIntersection;
            
            

            //Vector2 solution1 = midpt + v1;
            Vector2 solution1 = new Vector2();
            solution1.x =         midpt.x + v1.x;
            solution1.y =         midpt.y + v1.y;
            
            
            if ((solution1.x - x1_) * v1.x + (solution1.y - y1_) * v1.y > 0)
            {
                m_result.InsertSolution(solution1);
            }
            
            //Vector2 solution2 = midpt - v1;
            Vector2 solution2 = new Vector2();
            solution2.x =midpt.x - v1.x;
            solution2.y =midpt.y - v1.y;
            if ((solution2.x - x1_) * v1.x + (solution2.y - y1_) * v1.y > 0)
            {
                m_result.InsertSolution(solution2);
            }
            
            return m_result;
        }
        
       //Line to Line
        public IntersectionObject LineToLineIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                                float x3_, float y3_, float x4_, float y4_)
        {
            m_result = new IntersectionObject();
            float r, s, d;
            //Make sure the lines aren't parallel
            if ((y2_ - y1_) / (x2_ - x1_) != (y4_ - y3_) / (x4_ - x3_))
            {
                d = (((x2_ - x1_) * (y4_ - y3_)) - (y2_ - y1_) * (x4_ - x3_));
                if (d != 0)
                {
                    r = (((y1_ - y3_) * (x4_ - x3_)) - (x1_ - x3_) * (y4_ - y3_)) / d;
                    s = (((y1_ - y3_) * (x2_ - x1_)) - (x1_ - x3_) * (y2_ - y1_)) / d;

                    m_result.InsertSolution(x1_ + r * (x2_ - x1_), y1_ + r * (y2_ - y1_));
                }
            }
            return m_result;
        }

        //LineSegment to LineSegment
        public IntersectionObject LineSegmentToLineSegmentIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                                              float x3_, float y3_, float x4_, float y4_)
        {
            m_result = new IntersectionObject();
            float r, s, d;
            //Make sure the lines aren't parallel
            if ((y2_ - y1_) / (x2_ - x1_) != (y4_ - y3_) / (x4_ - x3_))
            {
                d = (((x2_ - x1_) * (y4_ - y3_)) - (y2_ - y1_) * (x4_ - x3_));
                if (d != 0)
                {
                    r = (((y1_ - y3_) * (x4_ - x3_)) - (x1_ - x3_) * (y4_ - y3_)) / d;
                    s = (((y1_ - y3_) * (x2_ - x1_)) - (x1_ - x3_) * (y2_ - y1_)) / d;
                    if (r >= 0 && r <= 1)
                    {
                        if (s >= 0 && s <= 1)
                        {
                            m_result.InsertSolution(x1_ + r * (x2_ - x1_), y1_ + r * (y2_ - y1_));
                        }
                    }
                }
            }
            return m_result;
        }

        //Line to LineSement
        public IntersectionObject LineToLineSegmentIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                                       float x3_, float y3_, float x4_, float y4_)
        {
            return LineSegmentToLineIntersection(x3_, y3_, x4_, y4_, x1_, y1_, x2_, y2_);
        }

        //LineSegment to Line
        public IntersectionObject LineSegmentToLineIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                                       float x3_, float y3_, float x4_, float y4_)
        {
            m_result = new IntersectionObject();
            float r, s, d;
            //Make sure the lines aren't parallel
            if ((y2_ - y1_) / (x2_ - x1_) != (y4_ - y3_) / (x4_ - x3_))
            {
                d = (((x2_ - x1_) * (y4_ - y3_)) - (y2_ - y1_) * (x4_ - x3_));
                if (d != 0)
                {
                    r = (((y1_ - y3_) * (x4_ - x3_)) - (x1_ - x3_) * (y4_ - y3_)) / d;
                    s = (((y1_ - y3_) * (x2_ - x1_)) - (x1_ - x3_) * (y2_ - y1_)) / d;
                    if (r >= 0 && r <= 1)
                    {
                        m_result.InsertSolution(x1_ + r * (x2_ - x1_), y1_ + r * (y2_ - y1_));
                    }
                }
            }
            return m_result;
        }

        //LineSegment to Ray
        public IntersectionObject LineSegmentToRayIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                                      float x3_, float y3_, float x4_, float y4_)
        {
            return RayToLineSegmentIntersection(x3_, y3_, x4_, y4_, x1_, y1_, x2_, y2_);
        }

        //Ray to LineSegment
        public IntersectionObject RayToLineSegmentIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                                      float x3_, float y3_, float x4_, float y4_)
        {
            m_result = new IntersectionObject();
            float r, s, d;
            //Make sure the lines aren't parallel
            if ((y2_ - y1_) / (x2_ - x1_) != (y4_ - y3_) / (x4_ - x3_))
            {
                d = (((x2_ - x1_) * (y4_ - y3_)) - (y2_ - y1_) * (x4_ - x3_));
                if (d != 0)
                {
                    r = (((y1_ - y3_) * (x4_ - x3_)) - (x1_ - x3_) * (y4_ - y3_)) / d;
                    s = (((y1_ - y3_) * (x2_ - x1_)) - (x1_ - x3_) * (y2_ - y1_)) / d;
                    if (r >= 0)
                    {
                        if (s >= 0 && s <= 1)
                        {
                            m_result.InsertSolution(x1_ + r * (x2_ - x1_), y1_ + r * (y2_ - y1_));
                        }
                    }
                }
            }
            return m_result;
        }

        //Line to Ray
        public IntersectionObject LineToRayIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                               float x3_, float y3_, float x4_, float y4_)
        {
            return RayToLineIntersection(x3_, y3_, x4_, y4_, x1_, y1_, x2_, y2_);
        }

        //Ray to Line
        public IntersectionObject RayToLineIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                               float x3_, float y3_, float x4_, float y4_)
        {
            m_result = new IntersectionObject();
            float r, s, d;
            //Make sure the lines aren't parallel
            if ((y2_ - y1_) / (x2_ - x1_) != (y4_ - y3_) / (x4_ - x3_))
            {
                d = (((x2_ - x1_) * (y4_ - y3_)) - (y2_ - y1_) * (x4_ - x3_));
                if (d != 0)
                {
                    r = (((y1_ - y3_) * (x4_ - x3_)) - (x1_ - x3_) * (y4_ - y3_)) / d;
                    s = (((y1_ - y3_) * (x2_ - x1_)) - (x1_ - x3_) * (y2_ - y1_)) / d;
                    if (r >= 0)
                    {
                        m_result.InsertSolution(x1_ + r * (x2_ - x1_), y1_ + r * (y2_ - y1_));
                    }
                }
            }
            return m_result;
        }

       //Ray to Ray
        public IntersectionObject RayToRayIntersection(float x1_, float y1_, float x2_, float y2_, 
                                                              float x3_, float y3_, float x4_, float y4_)
        {
            m_result = new IntersectionObject();
            float r, s, d;
            //Make sure the lines aren't parallel
            if ((y2_ - y1_) / (x2_ - x1_) != (y4_ - y3_) / (x4_ - x3_))
            {
                d = (((x2_ - x1_) * (y4_ - y3_)) - (y2_ - y1_) * (x4_ - x3_));
                if (d != 0)
                {
                    r = (((y1_ - y3_) * (x4_ - x3_)) - (x1_ - x3_) * (y4_ - y3_)) / d;
                    s = (((y1_ - y3_) * (x2_ - x1_)) - (x1_ - x3_) * (y2_ - y1_)) / d;
                    if (r >= 0)
                    {
                        if (s >= 0)
                        {
                            m_result.InsertSolution(x1_ + r * (x2_ - x1_), y1_ + r * (y2_ - y1_));
                        }
                    }
                }
            }
            return m_result;
        }        
        
        public IntersectionObject getMidPoint(float x1_, float y1_, float x2_, float y2_)
        {
            m_result = new IntersectionObject();
            
            m_result.InsertSolution((x2_+x1_)/2, (y2_+y1_)/2);
            
            return m_result;
        }
        
        public double getAngleInDegree(float x1_, float y1_, float x2_, float y2_, 
                                                              float x3_, float y3_, float x4_, float y4_)
        {
            
            double angle1 = Math.atan2((y2_ - y1_), (x2_ - x1_));
            double angle2 = Math.atan2((y4_- y3_),   (x4_ - x3_));
            double angleD = (angle1-angle2)*180.0d/Math.PI;
            return angleD;
        }
        
        public double getAngleWithXAxisInDegree(float x1_, float y1_, float x2_, float y2_)
        {
            double angle1 = Math.atan2((y2_ - y1_), (x2_ - x1_));
            double angleD = angle1*180.0d/Math.PI;
            return angleD;
        }
        
}
