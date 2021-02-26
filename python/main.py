import matplotlib
import matplotlib.pyplot as plt
import numpy as np

from scipy import optimize

def position_fit(x, a, b, c, d, e):
       result = a + b * x * x + c * x * x * x * x + d * x * x * x * x * x * x + e * x * x * x * x * x * x * x * x
       return result

def position_fit32(x, a, b, c, d, e):
       a32 = np.float32(a)
       b32 = np.float32(b)
       c32 = np.float32(c)
       d32 = np.float32(d)
       e32 = np.float32(e)
       x32 = np.float32(x);
       v_sqr = x32 * x32
       v_cube = v_sqr * v_sqr

       r1 = a32 + (b32 + c32 * v_sqr) * v_sqr
       r2 = (d32 + e32 * v_sqr) * v_cube * v_sqr

       result = r1 + r2
       return result

def position_fit_log(x, a, b, c, d):
       result = np.log(position_fit(x, a, b, c, d))
       return result

def get_positions(tick_count):
       positions = np.empty(tick_count)
       position = 0.0
       velocity = 0.0
       for tick in range(tick_count):
              position += velocity
              positions[tick] = position
              velocity = (velocity - 0.08) * 0.98
       return positions

def get_velocity(tick_count):
       velocities = np.empty(tick_count)
       velocity = 0.0
       for tick in range(tick_count):
              velocity = (velocity - 0.08) * 0.98
              velocities[tick] = velocity
       return velocities

def get_elytra_damage(elytra_velocity_deltas):
       elyta_damage = np.empty(elytra_velocity_deltas.size)
       for index in range(elytra_velocity_deltas.size):
              damage = -elytra_velocity_deltas[index] * 10 - 3
              if damage > 0.0:
                     elyta_damage[index] = damage
              else:
                     elyta_damage[index] = 0.0
       return elyta_damage

tick_count = 500
ticks      = np.arange(0, tick_count, 1)
velocities = get_velocity(tick_count)
positions  = get_positions(tick_count)

vanilla_fall_damage = np.empty(tick_count)

for tick in ticks:
       if (positions[tick] < -3.0):
              vanilla_fall_damage[tick] = -3.0 - positions[tick]
       else:
              vanilla_fall_damage[tick] = 0.0

elytra_velocity_deltas = np.arange(0.0, -4.0, -0.01)
elytra_damage          = get_elytra_damage(elytra_velocity_deltas)

fit_positions  = []
fit_velocities = []


for index in range(positions.size):
       position = 1 - positions[index]
       if position < 48 and position < 256:
              fit_positions.append(position)
              fit_velocities.append(velocities[index])

fit_positions  = np.array(fit_positions)
fit_velocities = np.array(fit_velocities)

# params, params_covariance = optimize.curve_fit(position_fit_log, fit_velocities, np.log(fit_positions), p0=[1, 1, 1, 1])
params, params_covariance = optimize.curve_fit(position_fit, fit_velocities, fit_positions, p0=[1, 1, 1, 1, 1])

print(float.hex(params[0] - 1))
print(float.hex(params[1]))
print(float.hex(params[2]))
print(float.hex(params[3]))
print(float.hex(params[4]))

approx_positions = position_fit32(fit_velocities, params[0], params[1], params[2], params[3], params[4])

biggest_delta = 0

for index in range(0, fit_positions.size):
       fit_pos = fit_positions[index]
       app_pos = approx_positions[index]

       delta = fit_pos - app_pos

       if (delta > biggest_delta):
              biggest_delta = delta

print("delta")
print(float.hex(biggest_delta))

fig, ax = plt.subplots()
ax.plot(fit_velocities, fit_positions)
ax.plot(fit_velocities, approx_positions)

# ax.plot(velocities, fall_damage)
# ax.plot(elytra_velocity_deltas, elytra_damage)

ax.set(xlabel='velocity', ylabel='position', title='')

ax.grid()

fig.savefig("test.png")
plt.show()