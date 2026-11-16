import { IsBoolean, IsNotEmpty, IsNumber, IsOptional, IsString, Max, Min } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateGeofenceDto {
  @ApiProperty({ example: 'Home' })
  @IsString()
  @IsNotEmpty()
  name: string;

  @ApiProperty({ example: 37.7749 })
  @IsNumber()
  @Min(-90)
  @Max(90)
  latitude: number;

  @ApiProperty({ example: -122.4194 })
  @IsNumber()
  @Min(-180)
  @Max(180)
  longitude: number;

  @ApiProperty({ example: 250, description: 'Geofence radius in meters' })
  @IsOptional()
  @IsNumber()
  @Min(50)
  @Max(5000)
  radiusMeters?: number;

  @ApiProperty({ example: true })
  @IsOptional()
  @IsBoolean()
  notifyOnExit?: boolean;

  @ApiProperty({ example: true })
  @IsOptional()
  @IsBoolean()
  notifyOnEnter?: boolean;
}
