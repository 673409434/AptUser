package com.dev.hh.aptuser.aptdemo;

import com.hh.annotation.factory.Factory;

/**
 * Package: com.dev.hh.aptuser.aptuser
 * User: hehao3
 * Email: hehao3@jd.com
 * Date: 2021/4/12
 * Time: 上午10:32
 * Description:
 */


@Factory(id = "Rectangle", type = IShape.class)
public class Rectangle implements IShape {
    public Rectangle() {
    }
    @Override
    public void draw() {
        System.out.println("Draw a Rectangle");
    }
}