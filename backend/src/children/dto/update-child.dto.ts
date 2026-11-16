import { IsString, IsDateString, IsOptional, IsUrl } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class UpdateChildDto {
  @ApiProperty({ example: 'Alex Johnson', required: false })
  @IsOptional()
  @IsString()
  name?: string;

  @ApiProperty({ example: '2014-06-15', required: false })
  @IsOptional()
  @IsDateString()
  birthDate?: string;

  @ApiProperty({ example: 'https://images.unsplash.com/avatar-child-2.png', required: false })
  @IsOptional()
  @IsUrl()
  avatarUrl?: string;
}
