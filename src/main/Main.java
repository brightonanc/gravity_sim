package main;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

public class Main {
	private static GLFWErrorCallback errorCallback;
	private static GLFWKeyCallback keyCallback;
	private static GLFWFramebufferSizeCallback resizeCallback;
	private static GLFWMouseButtonCallback mouseButtonCallback;
	private static GLFWCursorPosCallback cursorPosCallback;
	private static GLFWScrollCallback scrollCallback;
	
	public static final double NaN = Double.NaN;
	
	public static final int INIT_WIDTH = 700;
	public static final int INIT_HEIGHT = 680;
	
	public static boolean pressed = false;
	public static int pressedMass = -1;
	public static double pressedX = NaN;
	public static double pressedY = NaN;
	public static double currentX = NaN;
	public static double currentY = NaN;
	
	public static double fL = -1D;
	public static double fR = 1D;
	public static double fB = -1D;
	public static double fT = 1D;
	
	public static boolean dragged = false;
	public static double draggedX = NaN;
	public static double draggedY = NaN;
	
	public static boolean prevResizePriority = true;
	public static int prevResizeWorH = INIT_HEIGHT;
 
	// The window handle
	private static long window;
 
	public static void run() {
 
		try {
			init();
			loop();
			GLFW.glfwDestroyWindow(window);
			keyCallback.release();
		} finally {
			GLFW.glfwTerminate();
			errorCallback.release();
		}
	}
 
	private static void init() {
		GLFW.glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
 
		if (GLFW.glfwInit() != GLFW.GLFW_TRUE )
			throw new IllegalStateException("Unable to initialize GLFW");
 
		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
 
		window = GLFW.glfwCreateWindow(INIT_WIDTH, INIT_HEIGHT, "Gravity Simulator ~ Brighton Ancelin 2015", MemoryUtil.NULL, MemoryUtil.NULL);
		if(window == MemoryUtil.NULL)
			throw new RuntimeException("Failed to create the GLFW window");
 
		GLFW.glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if(key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE)
					GLFW.glfwSetWindowShouldClose(window, GLFW.GLFW_TRUE);
			}
		});
		
		GLFW.glfwSetMouseButtonCallback(window, mouseButtonCallback = new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {
				DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
				DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
				GLFW.glfwGetCursorPos(window, xBuffer, yBuffer);
				double x = xBuffer.get(0);
				double y = yBuffer.get(0);
				IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
				IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
				GLFW.glfwGetFramebufferSize(window, widthBuffer, heightBuffer);
				double width = widthBuffer.get(0);
				double height = heightBuffer.get(0);
				x /= width;
				y /= height;
				x = (x * (fR - fL)) + fL;
				y = fT - (y * (fT - fB));
				if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !dragged) {
					if(action == GLFW.GLFW_PRESS) {
						Main.pressed = true;
						Main.pressedMass = 10;
						Main.pressedX = Main.currentX = x;
						Main.pressedY = Main.currentY = y;
					} else if(action == GLFW.GLFW_RELEASE) {
						double velX = (x - Main.pressedX) * 50D;
						double velY = (y - Main.pressedY) * 50D;
						Main.createBody(Main.pressedMass, Main.pressedX, Main.pressedY, velX, velY);
						Main.pressed = false;
					}
				} else if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !pressed) {
					if(action == GLFW.GLFW_PRESS) {
						Main.dragged = true;
						Main.draggedX = x;
						Main.draggedY = y;
					} else if(action == GLFW.GLFW_RELEASE) {
						Main.dragged = false;
					}
				}
			}
		});
		
		GLFW.glfwSetCursorPosCallback(window, cursorPosCallback = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double xpos, double ypos) {
				double x = xpos;
				double y = ypos;
				IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
				IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
				GLFW.glfwGetFramebufferSize(window, widthBuffer, heightBuffer);
				double width = widthBuffer.get(0);
				double height = heightBuffer.get(0);
				x /= width;
				y /= height;
				x = (x * (fR - fL)) + fL;
				y = fT - (y * (fT - fB));
				if(Main.pressed) {
					Main.currentX = x;
					Main.currentY = y;
				} else if(Main.dragged) {
					fL -= (x - Main.draggedX) * 0.8D;
					fR -= (x - Main.draggedX) * 0.8D;
					fB -= (y - Main.draggedY) * 0.8D;
					fT -= (y - Main.draggedY) * 0.8D;
					Main.draggedX = x;
					Main.draggedY = y;
				}
			}
		});
		
		GLFW.glfwSetScrollCallback(window, scrollCallback = new GLFWScrollCallback() {
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				// Mouse scrollwheels are all in the y and are incremental (hence 'offset')
				if(Main.pressed) {
					Main.pressedMass = (int)(Main.pressedMass * Math.pow(1.1D, (int)yoffset));
				} else {
					final double scale = 1.1D;
					double new_fL = ((fL + fR) / 2D) - (((fR - fL) / 2D) * Math.pow(scale, (int)-yoffset));
					double new_fR = ((fL + fR) / 2D) + (((fR - fL) / 2D) * Math.pow(scale, (int)-yoffset));
					double new_fB = ((fB + fT) / 2D) - (((fT - fB) / 2D) * Math.pow(scale, (int)-yoffset));
					double new_fT = ((fB + fT) / 2D) + (((fT - fB) / 2D) * Math.pow(scale, (int)-yoffset));
					final double minOrtho = 0.1D;
					final double maxOrtho = 10D;
					if(Main.prevResizePriority) {
						if(new_fT - new_fB > minOrtho && new_fT - new_fB < maxOrtho) {
							fL = new_fL;
							fR = new_fR;
							fB = new_fB;
							fT = new_fT;
						}
					} else {
						if(new_fR - new_fL > minOrtho && new_fR - new_fL < maxOrtho) {
							fL = new_fL;
							fR = new_fR;
							fB = new_fB;
							fT = new_fT;
						}
					}
				}
			}
		});
 
		GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
		GLFW.glfwSetWindowPos(
			window,
			(vidmode.width() - INIT_WIDTH) / 2,
			(vidmode.height() - INIT_HEIGHT) / 2
		);
 
		GLFW.glfwMakeContextCurrent(window);
		GLFW.glfwSwapInterval(1);
 
		GLFW.glfwShowWindow(window);
		
		GL.createCapabilities();
		
		GLFW.glfwSetFramebufferSizeCallback(window, resizeCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				GL11.glViewport(0, 0, width, height);
				if(height < width) {
					double orthoPerPixel = Main.prevResizePriority ? (fT - fB) / Main.prevResizeWorH : (fR - fL) / Main.prevResizeWorH; 
					double new_fL = ((fL + fR) / 2D) - ((width * orthoPerPixel) / 2D);
					double new_fR = ((fL + fR) / 2D) + ((width * orthoPerPixel) / 2D);
					fL = new_fL;
					fR = new_fR;
					Main.prevResizePriority = true;
					Main.prevResizeWorH = height;
				} else {
					double orthoPerPixel = Main.prevResizePriority ? (fT - fB) / Main.prevResizeWorH : (fR - fL) / Main.prevResizeWorH; 
					double new_fB = ((fB + fT) / 2D) - ((height * orthoPerPixel) / 2D);
					double new_fT = ((fB + fT) / 2D) + ((height * orthoPerPixel) / 2D);
					fB = new_fB;
					fT = new_fT;
					Main.prevResizePriority = false;
					Main.prevResizeWorH = width;
				}
			}
		});
	}
 
	private static void loop() {
 
		while(GLFW.glfwWindowShouldClose(window) == GLFW.GLFW_FALSE) {
			//update objects
			for(int i = 0; i < bodies.size(); i++) {
				for(int j = i+1; j < bodies.size(); j++) {
					double thresholdRadius = 0.01D * Math.cbrt(bodies.get(i).mass);
					if((((bodies.get(i).x - bodies.get(j).x)*(bodies.get(i).x - bodies.get(j).x)) + ((bodies.get(i).y - bodies.get(j).y)*(bodies.get(i).y - bodies.get(j).y))) < thresholdRadius * thresholdRadius) {
						bodies.get(i).velX = ((bodies.get(i).mass * bodies.get(i).velX) + (bodies.get(j).mass * bodies.get(j).velX)) / (bodies.get(i).mass + bodies.get(j).mass);
						bodies.get(i).velY = ((bodies.get(i).mass * bodies.get(i).velY) + (bodies.get(j).mass * bodies.get(j).velY)) / (bodies.get(i).mass + bodies.get(j).mass);
						bodies.get(i).mass += bodies.get(j).mass;
						bodies.remove(j);
						j--;
					}
				}
			}
			double time = 0.0001D;
			for(int i = 0; i < bodies.size(); i++) {
				double accX = 0D;
				double accY = 0D;
				for(int j = 0; j < bodies.size(); j++) {
					if(j != i) {
						double baseAcc = (bodies.get(j).mass) / Math.pow((((bodies.get(i).x - bodies.get(j).x)*(bodies.get(i).x - bodies.get(j).x)) + ((bodies.get(i).y - bodies.get(j).y)*(bodies.get(i).y - bodies.get(j).y))), 1.5D);
						accX += baseAcc * (bodies.get(j).x - bodies.get(i).x);
						accY += baseAcc * (bodies.get(j).y - bodies.get(i).y);
					}
				}
				bodies.get(i).velX += accX * time;
				bodies.get(i).velY += accY * time;
			}
			for(int i = 0; i < bodies.size(); i++) {
				bodies.get(i).x += bodies.get(i).velX * time;
				bodies.get(i).y += bodies.get(i).velY * time;
			}
			
			//render
			
			GL11.glClearColor(0f, 0f, 0.0f, 0.0f);
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GL11.glOrtho(fL, fR, fB, fT, -1, 1);
			
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			
			GL11.glColor3f(1F, 0F, 0F);
			for(int i = 0; i < bodies.size(); i++) {
				circle((float)bodies.get(i).x, (float)bodies.get(i).y, bodies.get(i).getRadius());
			}
			if(Main.pressed) {
				GL11.glColor3f(0F, 1F, 0F);
				GL11.glBegin(GL11.GL_LINES);
				GL11.glVertex2d(Main.pressedX, Main.pressedY);
				GL11.glVertex2d(Main.currentX, Main.currentY);
				final double hookLength = 0.03D;
				double m1 = (Main.currentY - Main.pressedY) / (Main.currentX - Main.pressedX);
				double angle1 = Math.atan(m1) - (0.833D * Math.PI);
				double angle2 = Math.atan(m1) + (0.833D * Math.PI);
				double deltaXHook1 = hookLength * cos(angle1);
				double deltaYHook1 = hookLength * sin(angle1);
				double deltaXHook2 = hookLength * cos(angle2);
				double deltaYHook2 = hookLength * sin(angle2);
				if(Main.currentX - Main.pressedX < 0) {
					deltaXHook1 *= -1D;
					deltaYHook1 *= -1D;
					deltaXHook2 *= -1D;
					deltaYHook2 *= -1D;
				}
				GL11.glVertex2d(Main.currentX, Main.currentY);
				GL11.glVertex2d(Main.currentX + deltaXHook1, Main.currentY + deltaYHook1);
				GL11.glVertex2d(Main.currentX, Main.currentY);
				GL11.glVertex2d(Main.currentX + deltaXHook2, Main.currentY + deltaYHook2);
				GL11.glEnd();
				
				GL11.glColor3f(1F, 1F, 1F);
				circle((float)Main.pressedX, (float)Main.pressedY, Body.getRadius(Main.pressedMass));
			}
			
			GLFW.glfwSwapBuffers(window);
 
			GLFW.glfwPollEvents();
		}
	}
	
	private static void circle(float x, float y, float radius) {
		GL11.glBegin(GL11.GL_POLYGON);
		for(int i = 0; i < 120; i++) {
			GL11.glVertex2f(x + (radius * cos((float)(((double)i/120)*2*Math.PI))), y + (radius * sin((float)(((double)i/120)*2*Math.PI))));
		}
		GL11.glEnd();
	}
	
	private static int sineTablePrecision = 120;
	private static float[] sineTable = new float[sineTablePrecision];
	static {
		for(int i = 0; i < sineTablePrecision; i ++) {
			sineTable[i] = (float)Math.sin(0.5 * Math.PI * ((double)i/sineTablePrecision));
		}
	}
	private static float sin(double angle) {
		return sin((float)angle);
	}
	private static float sin(float angle) {
		final float PI = (float)Math.PI;
		float a = angle % PI;
		a = a < 0 ? a + PI : a;
		if(a > (0.5 * PI) && a < PI)
			a = PI - a;
		int sign = Math.floor(angle / PI) % 2 == 0 ? 1 : -1;
		a *= 2 * (sineTablePrecision / PI);
		int index = (int)Math.floor(a);
		index = index >= sineTablePrecision ? sineTablePrecision-1 : index;
		return sign * sineTable[index];
	}
	private static float cos(double angle) {
		return cos((float)angle);
	}
	private static float cos(float angle) {
		return sin(angle + (0.5F * (float)Math.PI));
	}
	
	private static List<Body> bodies = new ArrayList<Body>();
	
	private static class Body {
		public int mass;
		public double x;
		public double y;
		public double velX;
		public double velY;
		public Body(int mass, double x, double y, double velX, double velY) {
			this.mass = mass;
			this.x = x;
			this.y = y;
			this.velX = velX;
			this.velY = velY;
		}
		public static float getRadius(int mass) {
			return (float)Math.cbrt(mass) * 0.01F;
		}
		public float getRadius() {
			return getRadius(this.mass);
		}
	}
	public static void createBody(int mass, double x, double y, double velX, double velY) {
		if(mass >= 0)
			bodies.add(new Body(mass, x, y, velX, velY));
	}
	
	public static void main(String[] args) {
		createBody(100, 0D, 0D, 0D, 0D);
		createBody(20, 0D, 0.5D, 14.4914D, 0D);
		createBody(20, 0D, -0.5D, -14.4914D, 0D);
		run();
	}
 
}
