package com.lx.opencvuilts;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class OpenCVUtils {

    private static String TAG_REGULATE = "图像校正";

    private static Point center = new Point();

    private static double g_dst_hight;  //最终图像的高度

    private static double g_dst_width; //最终图像的宽度

    /**
     * 获取最大矩形
     */
    public static MatOfPoint findRectangle(Mat source) {
        try {
            Mat src = new Mat();

            Imgproc.cvtColor(source, src, Imgproc.COLOR_BGR2RGB);

            Mat blurred = src.clone();
            Imgproc.medianBlur(src, blurred, 9);

            Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            List<Mat> blurredChannel = new ArrayList<Mat>();
            blurredChannel.add(blurred);
            List<Mat> gray0Channel = new ArrayList<Mat>();
            gray0Channel.add(gray0);

            MatOfPoint2f approxCurve;

            double maxArea = 0;
            int maxId = -1;

            for (int c = 0; c < 3; c++) {
                int ch[] = {c, 0};
                Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

                int thresholdLevel = 1;
                for (int t = 0; t < thresholdLevel; t++) {
                    if (t == 0) {
                        Imgproc.Canny(gray0, gray, 10, 20, 3, true); // true ?
                        Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                    } else {
                        Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                                Imgproc.THRESH_BINARY,
                                (src.width() + src.height()) / 200, t);
                    }

                    Imgproc.findContours(gray, contours, new Mat(),
                            Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                    for (MatOfPoint contour : contours) {
                        MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                        double area = Imgproc.contourArea(contour);
                        approxCurve = new MatOfPoint2f();
                        Imgproc.approxPolyDP(temp, approxCurve,
                                Imgproc.arcLength(temp, true) * 0.02, true);

                        if (approxCurve.total() == 4 && area >= maxArea) {
                            double maxCosine = 0;

                            List<Point> curves = approxCurve.toList();
                            for (int j = 2; j < 5; j++) {

                                double cosine = Math.abs(angle(curves.get(j % 4),
                                        curves.get(j - 2), curves.get(j - 1)));
                                maxCosine = Math.max(maxCosine, cosine);
                            }

                            if (maxCosine < 0.3) {
                                maxArea = area;
                                maxId = contours.indexOf(contour);
                            }
                        }
                    }
                }
            }

            if (maxId >= 0) {
                Log.d("扫描","最大矩形");
                return contours.get(maxId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断清晰度
     *
     * @param image bitmap
     */
    public static boolean isBlurByOpenCV(Bitmap image) {
        int l = CvType.CV_8UC1;
        Mat matImage = new Mat();
        Utils.bitmapToMat(image, matImage);
        Mat matImageGrey = new Mat();
        Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY); // 图像灰度化
        Bitmap destImage;
        destImage = Bitmap.createBitmap(image);
        Mat dst2 = new Mat();
        Utils.bitmapToMat(destImage, dst2);
        Mat laplacianImage = new Mat();
        dst2.convertTo(laplacianImage, l);
        Imgproc.Laplacian(matImageGrey, laplacianImage, CvType.CV_8U); // 拉普拉斯变换
        Mat laplacianImage8bit = new Mat();
        laplacianImage.convertTo(laplacianImage8bit, l);
        Bitmap bmp = Bitmap
                .createBitmap(laplacianImage8bit.cols(), laplacianImage8bit.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(laplacianImage8bit, bmp);
        int[] pixels = new int[bmp.getHeight() * bmp.getWidth()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight()); // bmp为轮廓图
        int maxLap = -16777216;
        for (int pixel : pixels) {
            if (pixel > maxLap) {
                maxLap = pixel;
            }
        }
        int userOffset = -4881250; // 界线（严格性）降低一点
        int soglia = -6118750 + userOffset; // -6118750为广泛使用的经验值
        soglia += 6118750 + userOffset;
        maxLap += 6118750 + userOffset;
        return maxLap <= soglia;
    }

    /**
     * 判断清晰度
     *
     * @param picFilePath 地址
     */
    public static boolean isBlurByOpenCV(String picFilePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = true;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // 通过path得到一个不超过2000*2000的Bitmap
        Bitmap image = decodeSampledBitmapFromFile(picFilePath, options, 2000, 2000);
        return isBlurByOpenCV(image);
    }

    /**
     * 图像透射变换
     */
    public static Bitmap imageRegulate(Bitmap bitmap) {
        Bitmap bitmapLast = null;
        try {
            Mat source = new Mat();
            Utils.bitmapToMat(bitmap, source);

            Mat src = source.clone();
            Mat bkup = source.clone();
            Mat img = source.clone();

            Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2GRAY);//二值化
            Imgproc.GaussianBlur(img, img, new Size(5, 5), 0, 0);

            //获取自定义核
            Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                    new Size(3, 3)); //第一个参数MORPH_RECT表示矩形的卷积核，当然还可以选择椭圆形的、交叉型的
            //膨胀操作
            Imgproc.dilate(img, img, element);  //实现过程中发现，适当的膨胀很重要
            Imgproc.Canny(img, img, 30, 120);   //边缘提取

            List<MatOfPoint> contours = new ArrayList<>();
            List<MatOfPoint> f_contours = new ArrayList<>();
            //注意第5个参数为CV_RETR_EXTERNAL，只检索外框
            Imgproc.findContours(img, f_contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE); //找轮廓

            //求出面积最大的轮廓
            int max_area = 0;
            int index = 0;
            for (int i = 0; i < f_contours.size(); i++) {
                double tmparea = Math.abs(Imgproc.contourArea(f_contours.get(i)));
                if (tmparea > max_area) {
                    index = i;
                    max_area = (int) tmparea;
                }

            }
            contours.add(f_contours.get(index));

            for (int line_type = 1; line_type <= 3; line_type++) {
                Mat black = img.clone();
                black.setTo(new Scalar(0, 0, 0));
                Imgproc.drawContours(black, contours, 0, new Scalar(255, 255, 255), line_type);  //注意线的厚度，不要选择太细的

                Mat lines = new Mat();
                Mat lineNew = new Mat();
                List<Point> corners = new ArrayList<>();
                MatOfPoint2f approx = new MatOfPoint2f();

                int para = 10;
                int flag = 0;
                for (; para < 300; para++) {
                    lines.release();
                    lineNew.release();
                    corners.clear();
                    approx.release();
                    center.x = 0;
                    center.y = 0;

                    Imgproc.HoughLinesP(black, lines, 1, Math.PI / 180, para, 30, 10);

                    Set<Integer> ErasePt = new HashSet<>();
                    for (int i = 0; i < lines.rows(); i++) {
                        for (int j = i + 1; j < lines.rows(); j++) {
                            if (IsBadLine((int) Math.abs(lines.get(i, 0)[0] - lines.get(j, 0)[0]),
                                    (int) Math.abs(lines.get(i, 0)[1] - lines.get(j, 0)[1]))
                                    && IsBadLine((int) Math.abs(lines.get(i, 0)[2] - lines.get(j, 0)[2]),
                                    (int) Math.abs(lines.get(i, 0)[3] - lines.get(j, 0)[3]))) {
                                ErasePt.add(j);//将该坏线加入集合
                            }
                        }
                    }

                    Log.d(TAG_REGULATE, "坏线数---" + ErasePt.size());

                    for (int Num = 0; Num < lines.rows(); Num++) {
                        if (!ErasePt.contains(Num)) {
                            lineNew.push_back(lines.rowRange(Num, Num + 1));
                        }
                    }

                    Log.d(TAG_REGULATE, "好线数---" + lineNew.rows());

                    if (lineNew.rows() != 4) {
                        continue;
                    }
                    //计算直线的交点，保存在图像范围内的部分
                    for (int i = 0; i < lineNew.rows(); i++) {
                        for (int j = i + 1; j < lineNew.rows(); j++) {
                            Point pt = computeIntersect(lineNew.get(i, 0), lineNew.get(j, 0));
                            if (pt.x >= 0 && pt.y >= 0 && pt.x <= src.cols() && pt.y <= src
                                    .rows()) { //保证交点在图像的范围之内
                                corners.add(pt);
                            }
                        }
                    }
                    Log.d(TAG_REGULATE, "点数---" + corners.size());

                    if (corners.size() != 4) {
                        continue;
                    }

                    boolean IsGoodPoints = true;

                    // 保证点与点的距离足够大以排除错误点
                    for (int i = 0; i < corners.size(); i++) {
                        for (int j = i + 1; j < corners.size(); j++) {
                            double distance = Math
                                    .sqrt((corners.get(i).x - corners.get(j).x) * (corners.get(i).x - corners
                                            .get(j).x) + (corners.get(i).y - corners.get(j).y) * (corners.get(i).y
                                            - corners.get(j).y));
                            if (distance < 5) {
                                IsGoodPoints = false;
                            }
                        }
                    }
                    if (!IsGoodPoints) {
                        continue;
                    }

                    MatOfPoint2f corners_pts = new MatOfPoint2f(
                            corners.get(0),
                            corners.get(1),
                            corners.get(2),
                            corners.get(3)
                    );

                    Imgproc.approxPolyDP(corners_pts, approx, Imgproc.arcLength(corners_pts, true) * 0.02, true);

                    if (lineNew.rows() == 4 && corners.size() == 4 && approx.rows() == 4) {
                        flag = 1;
                        break;
                    }
                }

                Log.d(TAG_REGULATE, "flag---" + flag);
                // Get mass center
                Point center = new Point(0, 0);
                for (int i = 0; i < corners.size(); i++) {
                    center.x = center.x + corners.get(i).x;
                    center.y = center.y + corners.get(i).y;
                }
                center.x = center.x / corners.size();
                center.y = center.y / corners.size();

                if (flag == 1) {

                    Imgproc.circle(bkup, corners.get(0), 3, new Scalar(255, 0, 0), -1);
                    Imgproc.circle(bkup, corners.get(1), 3, new Scalar(0, 255, 0), -1);
                    Imgproc.circle(bkup, corners.get(2), 3, new Scalar(0, 0, 255), -1);
                    Imgproc.circle(bkup, corners.get(3), 3, new Scalar(255, 255, 255), -1);
                    Imgproc.circle(bkup, center, 3, new Scalar(255, 0, 255), -1);

                    corners = sortCorners(corners, center);

                    CalcDstSize(corners);

                    MatOfPoint2f corners_pts = new MatOfPoint2f(
                            corners.get(0),
                            corners.get(1),
                            corners.get(2),
                            corners.get(3)
                    );

                    Mat quad = Mat.zeros((int) g_dst_hight, (int) g_dst_width, CvType.CV_8UC3);
                    MatOfPoint2f quad_pts = new MatOfPoint2f(
                            new Point(0, 0),
                            new Point(quad.cols(), 0),
                            new Point(0, quad.rows()),
                            new Point(quad.cols(), quad.rows())
                    );

                    Mat transmtx = Imgproc.getPerspectiveTransform(corners_pts, quad_pts);
                    Imgproc.warpPerspective(source, quad, transmtx, quad.size());

//                    Core.rotate(quad, quad, Core.ROTATE_180);
                    bitmapLast = Bitmap.createBitmap(quad.cols(), quad.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(quad, bitmapLast);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmapLast;
    }

    /**
     * 图像透射变换 mat point
     */
    public static Bitmap imageRegulateMat(Mat source, List<Point> corners) {
        Bitmap bitmapLast = null;
        try {
            Point center = new Point(0, 0);
            for (int i = 0; i < corners.size(); i++) {
                center.x = center.x + corners.get(i).x;
                center.y = center.y + corners.get(i).y;
            }
            center.x = center.x / corners.size();
            center.y = center.y / corners.size();

            corners = sortCorners(corners, center);

            double h1 = Math.sqrt((corners.get(0).x - corners.get(3).x) * (corners.get(0).x - corners.get(3).x)
                    + (corners.get(0).y - corners.get(3).y) * (corners.get(0).y - corners.get(3).y));
            double h2 = Math.sqrt((corners.get(1).x - corners.get(2).x) * (corners.get(1).x - corners.get(2).x)
                    + (corners.get(1).y - corners.get(2).y) * (corners.get(1).y - corners.get(2).y));
            double g_dst_hight = Math.max(h1, h2);

            double w1 = Math.sqrt((corners.get(0).x - corners.get(1).x) * (corners.get(0).x - corners.get(1).x)
                    + (corners.get(0).y - corners.get(1).y) * (corners.get(0).y - corners.get(1).y));
            double w2 = Math.sqrt((corners.get(2).x - corners.get(3).x) * (corners.get(2).x - corners.get(3).x)
                    + (corners.get(2).y - corners.get(3).y) * (corners.get(2).y - corners.get(3).y));
            double g_dst_width = Math.max(w1, w2);

            MatOfPoint2f corners_pts = new MatOfPoint2f(
                    corners.get(0),
                    corners.get(1),
                    corners.get(2),
                    corners.get(3)
            );

            Mat quad = Mat.zeros((int) g_dst_hight, (int) g_dst_width, CvType.CV_8UC3);
            MatOfPoint2f quad_pts = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(quad.cols(), 0),
                    new Point(0, quad.rows()),
                    new Point(quad.cols(), quad.rows())
            );

            Mat transmtx = Imgproc.getPerspectiveTransform(corners_pts, quad_pts);
            Imgproc.warpPerspective(source, quad, transmtx, quad.size());
//            cvtColor(quad, quad, Imgproc.COLOR_BGR2GRAY);
            bitmapLast = Bitmap.createBitmap(quad.cols(), quad.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(quad, bitmapLast);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmapLast;
    }

    /**
     * 灰度 二值化 降噪 处理图像
     */
    public static Bitmap binaryZation(Bitmap bitmap) {
        Bitmap bitmapLast = null;
        try {
            Mat source = new Mat();
            Utils.bitmapToMat(bitmap, source);
            Mat mat = new Mat();
            Imgproc.cvtColor(source, mat, Imgproc.COLOR_BGR2GRAY);
            int BLACK = 0;
            int WHITE = 255;
            int ucThre = 0, ucThre_new = 127;
            int nBack_count, nData_count;
            int nBack_sum, nData_sum;
            int nValue;
            int i, j;

            int width = mat.width(), height = mat.height();
            //寻找最佳的阙值
            while (ucThre != ucThre_new) {
                nBack_sum = nData_sum = 0;
                nBack_count = nData_count = 0;

                for (j = 0; j < height; ++j) {
                    for (i = 0; i < width; i++) {
                        nValue = (int) mat.get(j, i)[0];

                        if (nValue > ucThre_new) {
                            nBack_sum += nValue;
                            nBack_count++;
                        } else {
                            nData_sum += nValue;
                            nData_count++;
                        }
                    }
                }

                nBack_sum = nBack_sum / nBack_count;
                nData_sum = nData_sum / nData_count;
                ucThre = ucThre_new;
                ucThre_new = (nBack_sum + nData_sum) / 2;
            }

            //二值化处理
            int nBlack = 0;
            int nWhite = 0;
            for (j = 0; j < height; ++j) {
                for (i = 0; i < width; ++i) {
                    nValue = (int) mat.get(j, i)[0];
                    if (nValue > ucThre_new) {
                        mat.put(j, i, WHITE);
                        nWhite++;
                    } else {
                        mat.put(j, i, BLACK);
                        nBlack++;
                    }
                }
            }

            // 确保白底黑字
            if (nBlack > nWhite) {
                for (j = 0; j < height; ++j) {
                    for (i = 0; i < width; ++i) {
                        nValue = (int) (mat.get(j, i)[0]);
                        if (nValue == 0) {
                            mat.put(j, i, WHITE);
                        } else {
                            mat.put(j, i, BLACK);
                        }
                    }
                }
            }
            Mat lastMat = eightRemoveNoise(mat, 1);
            bitmapLast = Bitmap.createBitmap(lastMat.cols(), lastMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(lastMat, bitmapLast);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmapLast;
    }

    /**
     * 8邻域降噪，又有点像9宫格降噪;即如果9宫格中心被异色包围，则同化 作用：降噪(默认白底黑字)
     *
     * @param src  Mat矩阵对象
     * @param pNum 阀值 默认取1即可
     */
    public static Mat eightRemoveNoise(Mat src, int pNum) {
        int i, j, m, n, nValue, nCount;
        int width = getImgWidth(src), height = getImgHeight(src);

        // 如果一个点的周围都是白色的，自己确实黑色的，同化
        for (j = 1; j < height - 1; j++) {
            for (i = 1; i < width - 1; i++) {
                nValue = getPixel(src, j, i);
                if (nValue == 0) {
                    nCount = 0;
                    // 比较(j , i)周围的9宫格，如果周围都是白色，同化
                    for (m = j - 1; m <= j + 1; m++) {
                        for (n = i - 1; n <= i + 1; n++) {
                            if (getPixel(src, m, n) == 0) {
                                nCount++;
                            }
                        }
                    }
                    if (nCount <= pNum) {
                        // 周围黑色点的个数小于阀值pNum,把自己设置成白色
                        setPixel(src, j, i, getWHITE());
                    }
                } else {
                    nCount = 0;
                    // 比较(j , i)周围的9宫格，如果周围都是黑色，同化
                    for (m = j - 1; m <= j + 1; m++) {
                        for (n = i - 1; n <= i + 1; n++) {
                            if (getPixel(src, m, n) == 0) {
                                nCount++;
                            }
                        }
                    }
                    if (nCount >= 8 - pNum) {
                        // 周围黑色点的个数大于等于(8 - pNum),把自己设置成黑色
                        setPixel(src, j, i, getBLACK());
                    }
                }
            }
        }
        return src;
    }

    /**
     * 作用：输入图像Mat矩阵对象，返回图像的宽度
     *
     * @param src Mat矩阵图像
     */
    private static int getImgWidth(Mat src) {
        return src.cols();
    }

    /**
     * 作用：输入图像Mat矩阵，返回图像的高度
     *
     * @param src Mat矩阵图像
     */
    private static int getImgHeight(Mat src) {
        return src.rows();
    }

    /**
     * 作用：获取图像(y,x)点的像素，我们只针对单通道(灰度图)
     *
     * @param src Mat矩阵图像
     * @param y   y坐标轴
     * @param x   x坐标轴
     */
    private static int getPixel(Mat src, int y, int x) {
        return (int) src.get(y, x)[0];
    }

    /**
     * 作用：设置图像(y,x)点的像素，我们只针对单通道(灰度图)
     *
     * @param src   Mat矩阵图像
     * @param y     y坐标轴
     * @param x     x坐标轴
     * @param color 颜色值[0-255]
     */
    private static void setPixel(Mat src, int y, int x, int color) {
        src.put(y, x, color);
    }

    private static int getBLACK() {
        return BLACK;
    }

    private static int getWHITE() {
        return WHITE;
    }

    private static void CalcDstSize(List<Point> corners) {
        double h1 = Math.sqrt((corners.get(0).x - corners.get(3).x) * (corners.get(0).x - corners.get(3).x)
                + (corners.get(0).y - corners.get(3).y) * (corners.get(0).y - corners.get(3).y));
        double h2 = Math.sqrt((corners.get(1).x - corners.get(2).x) * (corners.get(1).x - corners.get(2).x)
                + (corners.get(1).y - corners.get(2).y) * (corners.get(1).y - corners.get(2).y));
        g_dst_hight = Math.max(h1, h2);

        double w1 = Math.sqrt((corners.get(0).x - corners.get(1).x) * (corners.get(0).x - corners.get(1).x)
                + (corners.get(0).y - corners.get(1).y) * (corners.get(0).y - corners.get(1).y));
        double w2 = Math.sqrt((corners.get(2).x - corners.get(3).x) * (corners.get(2).x - corners.get(3).x)
                + (corners.get(2).y - corners.get(3).y) * (corners.get(2).y - corners.get(3).y));
        g_dst_width = Math.max(w1, w2);
    }

    private static List<Point> sortCorners(List<Point> corners, Point center) {
        List<Point> top = new ArrayList<>();
        List<Point> bot = new ArrayList<>();
        List<Point> backup = corners;

        for (int i = 0; i < corners.size(); i++) {
            for (int j = i + 1; j < corners.size(); j++) {
                if (corners.get(i).x > corners.get(j).x) {
                    Point tmp = corners.get(i);
                    corners.set(i, corners.get(j));
                    corners.set(j, tmp);
                }
            }
        }

        for (int i = 0; i < corners.size(); i++) {
            if (corners.get(i).y < center.y && top.size() < 2) {
                top.add(corners.get(i));
            } else {
                bot.add(corners.get(i));
            }
        }

        corners.clear();

        if (top.size() == 2 && bot.size() == 2) {
            Point tl = top.get(0).x > top.get(1).x ? top.get(1) : top.get(0);
            Point tr = top.get(0).x > top.get(1).x ? top.get(0) : top.get(1);
            Point bl = bot.get(0).x > bot.get(1).x ? bot.get(1) : bot.get(0);
            Point br = bot.get(0).x > bot.get(1).x ? bot.get(0) : bot.get(1);

            corners.add(tl);
            corners.add(tr);
            corners.add(bl);
            corners.add(br);
        } else {
            corners = backup;
        }
        return corners;
    }

    private static Point computeIntersect(double[] a, double[] b) {
        double x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3];
        double x3 = b[0], y3 = b[1], x4 = b[2], y4 = b[3];
        double h1 = y2 - y1;
        double h2 = x2 * y1 - x1 * y2;
        double h3 = x2 - x1;
        double h4 = y4 - y3;
        double h5 = x4 * y3 - x3 * y4;
        double h6 = x4 - x3;

        double y = (h1 * h5 - h2 * h4) / (h1 * h6 - h3 * h4);
        double x = (y * h3 - h2) / h1;
        Point pt = new Point(x, y);
        return pt;
    }

    private static Boolean IsBadLine(int a, int b) {
        return (a * a + b * b < 100);
    }

    private static Bitmap decodeSampledBitmapFromFile(String imgPath, BitmapFactory.Options options, int reqWidth,
            int reqHeight) {
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath, options);
        // inSampleSize为缩放比例，举例：options.inSampleSize = 2表示缩小为原来的1/2，3则是1/3，以此类推
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imgPath, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
        System.out.println("inSampleSize=" + inSampleSize);
        return inSampleSize;
    }

    private static double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)
                / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
                + 1e-10);
    }
}
