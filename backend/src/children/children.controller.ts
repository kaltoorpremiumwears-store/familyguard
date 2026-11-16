import { Controller, Get, Post, Body, Put, Param, Delete, UseGuards, Request } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiResponse } from '@nestjs/swagger';
import { ChildrenService } from './children.service';
import { CreateChildDto } from './dto/create-child.dto';
import { UpdateChildDto } from './dto/update-child.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@ApiTags('Child Profiles')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('api/v1/children')
export class ChildrenController {
  constructor(private readonly childrenService: ChildrenService) {}

  @Post()
  @ApiOperation({ summary: 'Register a new child profile under parent' })
  @ApiResponse({ status: 201, description: 'Child profile and default policy created' })
  async create(@Request() req, @Body() dto: CreateChildDto) {
    return this.childrenService.create(req.user.parentId, dto);
  }

  @Get()
  @ApiOperation({ summary: 'List all children for authenticated parent' })
  async findAll(@Request() req) {
    return this.childrenService.findAllByParent(req.user.parentId);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get detailed child profile and active restrictions' })
  async findOne(@Request() req, @Param('id') id: string) {
    return this.childrenService.findOne(req.user.parentId, id);
  }

  @Put(':id')
  @ApiOperation({ summary: 'Update child profile details' })
  async update(@Request() req, @Param('id') id: string, @Body() dto: UpdateChildDto) {
    return this.childrenService.update(req.user.parentId, id, dto);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Permanently delete child profile and purge all associated records' })
  async remove(@Request() req, @Param('id') id: string) {
    return this.childrenService.remove(req.user.parentId, id);
  }
}
