package main;

class Point {
    float x, y;

	Point(float X, float Y) {
        x = X;
        y = Y;
    }

	void Add(Point point) {
        x += point.x;
        y += point.y;
    }

    Point Copy() {
        return new Point(x, y);
    }

    void CheckIsGreater(float value) {
        if (Math.abs(x) > value) {
            x = value * Math.signum(x);
        }
        if (Math.abs(y) > value) {
            y = value * Math.signum(y);
        }
    }

    boolean isClose(Point p, float distance) {
        return distance > Difference(p).Length();
    }

    Point Difference(Point p) {
        return new Point(x - p.x, y - p.y);
    }

    double Length() {
        return Math.sqrt(x * x + y * y);
    }

    Point Multiply(float multiplier) {
        return new Point(x * multiplier, y * multiplier);
    }

    Point Normalize() {
        double length = Length();
        return new Point((float)(x / length), (float)(y / length));
    }

    @Override
    public int hashCode() {
        return (int) (x + y * 2000);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) return false;
        Point p = (Point) o;
        return x == p.x && y == p.y;
    }
}
