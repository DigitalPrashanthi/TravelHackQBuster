package com.smartsay.qbuster.LibUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.smartsay.qbuster.R;

public class RadarScanView extends View {
    private static final int DEFAULT_HEIGHT = 300;
    private static final int DEFAULT_WIDTH = 300;
    /* access modifiers changed from: private */
    public volatile boolean bStopScanAnimation;
    /* access modifiers changed from: private */
    public int centerX;
    /* access modifiers changed from: private */
    public int centerY;
    private int circleColor = Color.parseColor("#a2a2a2");
    private int defaultHeight;
    private int defaultWidth;
    /* access modifiers changed from: private */
    public Handler handler = new Handler();
    private Paint mPaintCircle;
    private Paint mPaintRadar;
    /* access modifiers changed from: private */
    public Matrix matrix;
    private int radarColor = Color.parseColor("#99a2a2a2");
    private int radarRadius;
    /* access modifiers changed from: private */
    public Runnable run = new Runnable() {
        public void run() {
            if (!RadarScanView.this.bStopScanAnimation) {
                RadarScanView.this.start = RadarScanView.this.start + 2;
                RadarScanView.this.matrix = new Matrix();
                RadarScanView.this.matrix.postRotate((float) RadarScanView.this.start, (float) RadarScanView.this.centerX, (float) RadarScanView.this.centerY);
                RadarScanView.this.postInvalidate();
                RadarScanView.this.handler.postDelayed(RadarScanView.this.run, 10);
            }
        }
    };
    Shader shader;
    /* access modifiers changed from: private */
    public int start;
    private int tailColor = Color.parseColor("#50aaaaaa");

    public RadarScanView(Context context) {
        super(context);
        init(null, context);
    }

    public RadarScanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, context);
    }

    public RadarScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, context);
    }

    @TargetApi(21)
    public RadarScanView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, context);
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.centerX = w / 2;
        this.centerY = h / 2;
        this.radarRadius = Math.min(w, h);
        this.shader = new SweepGradient((float) this.centerX, (float) this.centerY, 0, this.tailColor);
    }

    private void init(AttributeSet attrs, Context context) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RadarScanView);
            this.circleColor = ta.getColor(R.styleable.RadarScanView_circleColor, this.circleColor);
            this.radarColor = ta.getColor(R.styleable.RadarScanView_radarColor, this.radarColor);
            this.tailColor = ta.getColor(R.styleable.RadarScanView_tailColor, this.tailColor);
            ta.recycle();
        }
        initPaint();
        this.defaultWidth = dip2px(context, 300.0f);
        this.defaultHeight = dip2px(context, 300.0f);
        this.matrix = new Matrix();
        this.handler.post(this.run);
    }

    private void initPaint() {
        this.mPaintCircle = new Paint();
        this.mPaintCircle.setColor(this.circleColor);
        this.mPaintCircle.setAntiAlias(true);
        this.mPaintCircle.setStyle(Style.STROKE);
        this.mPaintCircle.setStrokeWidth(2.0f);
        this.mPaintRadar = new Paint();
        this.mPaintRadar.setColor(this.radarColor);
        this.mPaintRadar.setAntiAlias(true);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int resultWidth;
        int resultHeight;
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (modeWidth == 1073741824) {
            resultWidth = sizeWidth;
        } else {
            resultWidth = this.defaultWidth;
            if (modeWidth == Integer.MIN_VALUE) {
                resultWidth = Math.min(resultWidth, sizeWidth);
            }
        }
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (modeHeight == 1073741824) {
            resultHeight = sizeHeight;
        } else {
            int resultHeight2 = this.defaultHeight;
            if (modeHeight == Integer.MIN_VALUE) {
                resultHeight = Math.min(resultHeight2, sizeHeight);
            } else {
                resultHeight = resultHeight2;
            }
        }
        setMeasuredDimension(resultWidth, resultHeight);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle((float) this.centerX, (float) this.centerY, (float) (this.radarRadius / 7), this.mPaintCircle);
        canvas.drawCircle((float) this.centerX, (float) this.centerY, (float) (this.radarRadius / 4), this.mPaintCircle);
        canvas.drawCircle((float) this.centerX, (float) this.centerY, (float) (this.radarRadius / 3), this.mPaintCircle);
        canvas.drawCircle((float) this.centerX, (float) this.centerY, (float) ((this.radarRadius * 3) / 7), this.mPaintCircle);
        if (this.shader == null) {
            this.shader = new SweepGradient((float) this.centerX, (float) this.centerY, 0, this.tailColor);
        }
        this.mPaintRadar.setShader(this.shader);
        canvas.concat(this.matrix);
        canvas.drawCircle((float) this.centerX, (float) this.centerY, (float) ((this.radarRadius * 3) / 7), this.mPaintRadar);
    }

    private int dip2px(Context context, float dipValue) {
        return (int) ((dipValue * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    private int px2dip(Context context, float pxValue) {
        return (int) ((pxValue / context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public void StopScanAnimation() {
        this.bStopScanAnimation = true;
        this.shader = new SweepGradient(0.0f, 0.0f, 0, this.tailColor);
    }

    public void StartScanAnimation() {
        if (this.bStopScanAnimation) {
            this.bStopScanAnimation = false;
            this.handler.post(this.run);
        }
        this.shader = new SweepGradient((float) this.centerX, (float) this.centerY, 0, this.tailColor);
    }
}
